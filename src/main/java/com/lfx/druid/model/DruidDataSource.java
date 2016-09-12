package com.lfx.druid.model;

import com.lfx.util.BashUtils;
import com.lfx.druid.model.util.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午2:12.
 */
public class DruidDataSource extends DruidAbstractDataSource {

    private final static Logger LOG = LoggerFactory.getLogger(DruidDataSource.class);

    private final CountDownLatch initedLatch = new CountDownLatch(2);
    private CreateConnectionThread createConnectionThread;
    private DestroyConnectionThread destroyConnectionThread;
    private DestroyTask destroyTask;

    private volatile DruidConnectionHolder[] connections;

    //空闲连接总数
    //写入值都在同步块中
    private int poolingCount = 0;

    //被占用连接数
    //写入值都在同步块中
    private int activeCount = 0;

    //等待连接创建完毕发出notEmpty信号的线程数量
    //举例：有一个获取连接的请求线程,因为获取失败而阻塞则+1
    //写入值的代码都在同步块中
    private int notEmptyWaitThreadCount = 0;

    private int notEmptyWaitThreadPeak = 0;

    //获取连接发送错误的数量
    private final AtomicLong connectErrorCount = new AtomicLong();

    private volatile boolean closed = false;
    private volatile boolean enable = true;

    private long notEmptyWaitCount = 0L;
    private long notEmptyWaitNanos = 0L;

    public static ThreadLocal<Long> waitNanosLocal = new ThreadLocal<Long>();


    public DruidDataSource() {
        this(false);
    }

    public DruidDataSource(boolean lockFair) {
        super(lockFair);

        //configFromPropety
    }

    public void init() throws SQLException {
        if (inited) {
            return;
        }

        //TODO 为什么要有一个多余的操作，声明一个的局部变量？
        final ReentrantLock lock = this.lock;

        //TODO 不清楚为什么要设置可中断，什么场景下会调用本线程的interrupt()
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new SQLException("interrupt", e);
        }

        boolean init = false;

        try {
            //获取锁的过程中有可能容器已经初始化过了
            if (inited) {
                return;
            }

            validateConfig();

            driver = JdbcUtils.createDriver(driverClassLoader, driverClass);

            connections = new DruidConnectionHolder[maxActive];

            SQLException connectError = null;

            try {
                //init connections
                for (int i = 0, size = getInitialSize(); i < size; ++i) {
                    PhysicalConnectionInfo pyConnectInfo = createPhysicalConnection();
                    DruidConnectionHolder holder = new DruidConnectionHolder(this, pyConnectInfo);
                    connections[poolingCount] = holder;

                    incrementPoolingCount();
                }

            } catch (SQLException e) {
                LOG.error("init datasource error, url: " + jdbcUrl, e);
                connectError = e;
            }

            createAndStartCreatorThread();
            createAndStartDestroyThread();
            initedLatch.await();

            init = true;
            initedTime = new Date();

            //初始化connections时，出现错误也要执行后面的代码,所以才在后面返回
            if (connectError != null && poolingCount == 0) {
                throw connectError;
            }
        } catch (SQLException e) {
            LOG.error("dataSource init error", e);
            throw e;
        } catch (InterruptedException e) {
            throw new SQLException(e.getMessage(), e);
        } finally {
            inited = true;
            lock.unlock();

            //日志打印只需要判断当前线程是否init,所以用局部变量
            if (init && LOG.isInfoEnabled()) {
                LOG.info("dataSource inited");
            }
        }
    }

    protected void createAndStartDestroyThread() {
        destroyTask = new DestroyTask();

        String threadName = "Druid-ConnectionPool-Destroy-" + System.identityHashCode(this);
        destroyConnectionThread = new DestroyConnectionThread(threadName);
        destroyConnectionThread.start();
    }

    protected void createAndStartCreatorThread() {
        String threadName = "Druid-ConnectionPool-Create-" + System.identityHashCode(this);
        createConnectionThread = new CreateConnectionThread(threadName);
        createConnectionThread.start();
    }

    private void validateConfig() throws SQLException {
        if (maxActive <= 0) {
            throw new IllegalArgumentException("illegal maxActive " + maxActive);
        }

        if (maxActive < minIdle) {
            throw new IllegalArgumentException("illegal maxActive " + maxActive);
        }

        if (getInitialSize() > maxActive) {
            throw new IllegalArgumentException("illegal initialSize " + this.initialSize + ", maxActieve " + maxActive);
        }

        if (maxEvictableIdleTimeMillis < minEvictableIdleTimeMillis) {
            throw new SQLException("maxEvictableIdleTimeMillis must be grater than minEvictableIdleTimeMillis");
        }
    }

    private final void decrementPoolingCount() {
        poolingCount--;
    }

    private final void incrementPoolingCount() {
        poolingCount++;
    }

    public class CreateConnectionThread extends Thread {
        public CreateConnectionThread(String name) {
            super(name);
            //后台线程
            this.setDaemon(true);
        }

        public void run() {
            initedLatch.countDown();

            int errorCount = 0;
            for (; ; ) {

                //同步块中判断是否要将当前线程wait，等待empty信号唤醒
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    break;
                }

                try {
                    boolean emptyWait = true;

                    //之前创建连接发生错误，且池中没有可用的连接则不需要wait，直接创建连接
                    if (createError != null && poolingCount == 0) {
                        emptyWait = false;
                    }

                    if (emptyWait) {
                        //比如：池中还有5个连接而有5个线程在等待锁来获取连接，这时如果再加一个等待线程则连接不够需要创建连接，
                        // 够用则不需要创建，阻塞创建线程，等待empty信号
                        if (poolingCount >= notEmptyWaitThreadCount) {
//                            System.out.println("createThread wait() notEmptyWaitThreadCount");
                            empty.await();
//                            BashUtils.executeJstack("dump.out");
                        }

                        // 防止创建超过maxActive数量的连接
                        if (activeCount + poolingCount >= maxActive) {
//                            System.out.println("createThread wait() maxActive");
                            empty.await();
//                            BashUtils.executeJstack("dump.out");
                            continue;
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } finally {
                    lock.unlock();
                }

                PhysicalConnectionInfo connection = null;

                try {
                    connection = createPhysicalConnection();
                    failContinuous.set(false);
                } catch (SQLException e) {
                    LOG.error("create connection error, url: " + jdbcUrl + ", errorCode " + e.getErrorCode() + ", state " + e.getSQLState(), e);

                    errorCount++;

                    //连续30(默认)次创建连接失败,并设置了(每30次与30次之间)失败间隔时间
                    if (errorCount > connectionErrorRetryAttempts && timeBetweenConnectErrorMillis > 0) {
                        //fail over retry attempts
                        failContinuous.set(true);
                        if (failFast) {
                            lock.lock();
                            try {
                                notEmpty.signalAll();
                            } finally {
                                lock.unlock();
                            }
                        }

                        if (breakAfterAcquireFailure) {
                            break;
                        }

                        //休眠一段时间后继续创建
                        try {
                            Thread.sleep(timeBetweenConnectErrorMillis);
                        } catch (InterruptedException interruptEx) {
                            break;
                        }
                    }

                } catch (RuntimeException e) {
                    LOG.error("create connection error", e);
                    failContinuous.set(true);
                    continue;
                } catch (Error e) {
                    LOG.error("create connection error", e);
                    failContinuous.set(true);
                    break;
                }

                if (connection == null) {
                    continue;
                }

                boolean result = put(connection);
                if (!result) {
                    JdbcUtils.close(connection.getPhysicalConnection());
                    LOG.info("put physical connection to pool failed.");
                }
                errorCount = 0;
            }
        }
    }

    protected boolean put(PhysicalConnectionInfo physicalConnectionInfo) {
        DruidConnectionHolder holder = null;
        try {
            holder = new DruidConnectionHolder(DruidDataSource.this, physicalConnectionInfo);
        } catch (SQLException ex) {
            LOG.error("create connection holder error", ex);
            return false;
        }

        lock.lock();
        try {
            if (poolingCount >= maxActive) {
                return false;
            }
            connections[poolingCount] = holder;
            incrementPoolingCount();


            notEmpty.signal();
//            notEmptySignalCount++;

        } finally {
            lock.unlock();
        }

        return true;
    }

    public class DestroyConnectionThread extends Thread {

        public DestroyConnectionThread(String name) {
            super(name);
            this.setDaemon(true);
        }

        public void run() {
            initedLatch.countDown();

            for (; ; ) {
                // 从前面开始删除
                try {
                    if (closed) {
                        break;
                    }

                    if (timeBetweenEvictionRunsMillis > 0) {
                        Thread.sleep(timeBetweenEvictionRunsMillis);
                    } else {
                        Thread.sleep(1000); //
                    }

                    if (Thread.interrupted()) {
                        break;
                    }

                    destroyTask.run();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

    }

    public class DestroyTask implements Runnable {

        public void run() {
            System.out.println("task.run(),cuurentThread: " + Thread.currentThread().getName());
            shrink(true);
        }

    }

    /**
     * 先删数组连接，再关闭连接。
     * checkCount = poolingCount - minIdle
     * 不检测时间：顺序关闭checkCount个连接
     * 检测时间：
     * 1、创建连接时间超过phyConnectTimeMillis则全部删除；
     * 2、数组中某个连接空闲时间小于minEvictableIdleTimeMillis,则删除前面所有连接，因为连接获取是逆序的，后面的连接空闲时间铁定在范围内；
     * 3、顺序关闭checkCount个连接 + 关闭空闲时间超过maxEvictableIdleTimeMillis的连接；
     *
     * @param checkTime
     */
    public void shrink(boolean checkTime) {
        final List<DruidConnectionHolder> evictList = new ArrayList<DruidConnectionHolder>();
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }

        try {
            final int checkCount = poolingCount - minIdle;
            final long currentTimeMillis = System.currentTimeMillis();

            //从头部开始检测，只要发现一个活跃的连接则退出检测功能
            for (int i = 0; i < poolingCount; ++i) {
                DruidConnectionHolder connection = connections[i];

                if (checkTime) {
                    if (phyTimeoutMillis > 0) {
                        long phyConnectTimeMillis = currentTimeMillis - connection.getConnectTimeMillis();
                        if (phyConnectTimeMillis > phyTimeoutMillis) {
                            evictList.add(connection);
                            continue;
                        }
                    }

                    long idleMillis = currentTimeMillis - connection.getLastActiveTimeMillis();

                    //空闲时间小于minEvictableIdleTimeMillis的连接就不用删除了
                    if (idleMillis < minEvictableIdleTimeMillis) {
                        break;
                    }

                    if (checkTime && i < checkCount) {
                        evictList.add(connection);
                    } else if (idleMillis > maxEvictableIdleTimeMillis) {
                        evictList.add(connection);
                    }
                } else {
                    if (i < checkCount) {
                        evictList.add(connection);
                    } else {
                        break;
                    }
                }
            }

            int removeCount = evictList.size();
            if (removeCount > 0) {
                System.arraycopy(connections, removeCount, connections, 0, poolingCount - removeCount);
                Arrays.fill(connections, poolingCount - removeCount, poolingCount, null);
                poolingCount -= removeCount;
            }
        } finally {
            lock.unlock();
        }

        //关闭连接
        for (DruidConnectionHolder item : evictList) {
            Connection connection = item.getConnection();
            JdbcUtils.close(connection);
            destroyCount.incrementAndGet();
        }
    }

    public DruidPooledConnection getConnection() throws SQLException {
        return getConnection(maxWait);
    }

    public DruidPooledConnection getConnection(long maxWaitMillis) throws SQLException {
        init();

        return getConnectionDirect(maxWaitMillis);
    }

    public DruidPooledConnection getConnectionDirect(long maxWaitMillis) throws SQLException {
        int notFullTimeoutRetryCnt = 0;
        for (; ; ) {
            // handle notFullTimeoutRetry
            DruidPooledConnection poolableConnection;
            try {
                poolableConnection = getConnectionInternal(maxWaitMillis);
            } catch (GetConnectionTimeoutException ex) {
                if (notFullTimeoutRetryCnt <= this.notFullTimeoutRetryCount && !isFull()) {
                    notFullTimeoutRetryCnt++;
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("not full timeout retry : " + notFullTimeoutRetryCnt);
                    }
                    continue;
                }
                throw ex;
            }

            return poolableConnection;
        }
    }

    public boolean isFull() {
        lock.lock();
        try {
            return this.poolingCount + this.activeCount >= this.maxActive;
        } finally {
            lock.unlock();
        }
    }

    private DruidPooledConnection getConnectionInternal(long maxWait) throws SQLException {
        if (closed) {
            connectErrorCount.incrementAndGet();
            throw new DataSourceClosedException("dataSource already closed");
        }

        if (!enable) {
            connectErrorCount.incrementAndGet();
            throw new DataSourceDisableException();
        }

        final long nanos = TimeUnit.MILLISECONDS.toNanos(maxWait);
        final int maxWaitThreadCount = getMaxWaitThreadCount();

        DruidConnectionHolder holder;
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            connectErrorCount.incrementAndGet();
            throw new SQLException("interrupt", e);
        }

        try {
            if (maxWaitThreadCount > 0) {
                if (notEmptyWaitThreadCount >= maxWaitThreadCount) {
                    connectErrorCount.incrementAndGet();
                    throw new SQLException("maxWaitThreadCount " + maxWaitThreadCount + ", current wait Thread count "
                            + lock.getQueueLength());
                }
            }

            if (maxWait > 0) {
                holder = pollLast(nanos);
            } else {
                holder = takeLast();
            }

            if (holder != null) {
                activeCount++;
            }
        } catch (InterruptedException e) {
            connectErrorCount.incrementAndGet();
            throw new SQLException(e.getMessage(), e);
        } catch (SQLException e) {
            connectErrorCount.incrementAndGet();
            throw e;
        } finally {
            lock.unlock();
        }

        if (holder == null) {
            if (this.createError != null) {
                throw new GetConnectionTimeoutException("holder == null", createError);
            } else {
                throw new GetConnectionTimeoutException("holder == null");
            }
        }

        holder.incrementUseCount();

        DruidPooledConnection poolalbeConnection = new DruidPooledConnection(holder);
        return poolalbeConnection;
    }

    private DruidConnectionHolder pollLast(long nanos) throws InterruptedException, SQLException {
        long estimate = nanos;

        for (; ; ) {
            if (poolingCount == 0) {
                empty.signal(); // send signal to CreateThread create connection

                if (failFast && failContinuous.get()) {
                    throw new DataSourceNotAvailableException(createError);
                }

                notEmptyWaitThreadCount++;
                if (notEmptyWaitThreadCount > notEmptyWaitThreadPeak) {
                    notEmptyWaitThreadPeak = notEmptyWaitThreadCount;
                }

                try {
                    long startEstimate = estimate;
                    // signal by recycle or creator
                    estimate = notEmpty.awaitNanos(estimate); //

                    notEmptyWaitCount++;
                    notEmptyWaitNanos += (startEstimate - estimate);

                    if (!enable) {
                        connectErrorCount.incrementAndGet();
                        throw new DataSourceDisableException();
                    }
                } catch (InterruptedException ie) {
                    notEmpty.signal(); // propagate to non-interrupted thread
//                    notEmptySignalCount++;
                    throw ie;
                } finally {
                    notEmptyWaitThreadCount--;
                }

                if (poolingCount == 0) {
                    if (estimate > 0) {
                        continue;
                    }

                    waitNanosLocal.set(nanos - estimate);
                    return null;
                }

            }

            decrementPoolingCount();
            DruidConnectionHolder last = connections[poolingCount];
            connections[poolingCount] = null;

            return last;
        }
    }

    DruidConnectionHolder takeLast() throws InterruptedException, SQLException {
        try {
            while (poolingCount == 0) {
                // send signal to CreateThread create connection
                System.out.println("发出empty信号");
                empty.signal();

                if (failFast && failContinuous.get()) {
                    throw new DataSourceNotAvailableException(createError);
                }

                notEmptyWaitThreadCount++;
                if (notEmptyWaitThreadCount > notEmptyWaitThreadPeak) {
                    notEmptyWaitThreadPeak = notEmptyWaitThreadCount;
                }
                try {
                    // signal by recycle or creator
                    notEmpty.await();
                    BashUtils.executeJstack("dump.out");
                } finally {
                    notEmptyWaitThreadCount--;
                }
                notEmptyWaitCount++;

                if (!enable) {
                    connectErrorCount.incrementAndGet();
                    throw new DataSourceDisableException();
                }
            }
        } catch (InterruptedException ie) {
            notEmpty.signal(); // propagate to non-interrupted thread
//            notEmptySignalCount++;
            throw ie;
        }

        decrementPoolingCount();
        DruidConnectionHolder last = connections[poolingCount];
        connections[poolingCount] = null;

        return last;
    }

    public void close() {
        lock.lock();
        try {
            if (this.closed) {
                return;
            }

            if (!this.inited) {
                return;
            }

            if (createConnectionThread != null) {
                createConnectionThread.interrupt();
            }

            if (destroyConnectionThread != null) {
                destroyConnectionThread.interrupt();
            }


            for (int i = 0; i < poolingCount; ++i) {
                try {
                    DruidConnectionHolder connHolder = connections[i];

                    //关闭StatementPool
                    Connection physicalConnection = connHolder.getConnection();
                    physicalConnection.close();
                    connections[i] = null;
                } catch (Exception ex) {
                    LOG.warn("close connection error", ex);
                }
            }
            poolingCount = 0;

            enable = false;

            //唤醒所有在等待获取连接的线程,避免永远等待下去
            notEmpty.signalAll();
//            notEmptySignalCount++;

            this.closed = true;

        } finally {
            lock.unlock();
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("dataSource closed");
        }
    }

//    public static void main(String[] args) {
//        for (int i = 0; i < 10; ++i) {
//            System.out.println(i);
//        }
//    }


    /**
     * 有条件的putLast&notEmpty.signal()
     */
    public void recycle() {

    }
}