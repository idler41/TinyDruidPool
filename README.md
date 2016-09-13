# TinyDruidPool
TinyDruidPool用于学习数据库连接池Druid的项目，所有代码都仿照Druid编写，但TinyDruidPool只保留连接池功能。去除所有其它功能(如：配置文件载入、过滤器管理等)，经测试TinyDruidPool可正常运行。

官方文档(未成功加载见：doc/druid-pool.png)：
![](doc/druid-pool.png)

## 功能概述
###DruidAbstractDataSource.java

抽象类只是定义了一些重要的变量与锁的初始化。部分代码如下：

```java
    protected ReentrantLock  lock;
    //连接池非空信号
    protected Condition      notEmpty;
    //连接池为空信号
    protected Condition      empty;
    
    protected volatile int initialSize = DEFAULT_INITIAL_SIZE;
    protected volatile int maxActive = DEFAULT_MAX_ACTIVE_SIZE;
    protected volatile int minIdle = DEFAULT_MIN_IDLE;
    
    public static final int DEFAULT_INITIAL_SIZE = 0;
    public static final int DEFAULT_MAX_ACTIVE_SIZE = 8;
    public static final int DEFAULT_MIN_IDLE = 0;
    
    public DruidAbstractDataSource(boolean lockFair) {
        this.lock = new ReentrantLock((lockFair));
        this.notEmpty = lock.newCondition();
        this.empty = lock.newCondition();
    }    
```
### DruidDataSource.java
核心类，几乎包含连接池所有功能。

获取连接与归还连接都在connections尾部进行，而销毁连接则在connections头部进行。

```java
    //初始化时使用
    private final CountDownLatch initedLatch = new CountDownLatch(2);
	//创建连接的守护线程
    private CreateConnectionThread createConnectionThread;
    //关闭过期以及无用连接的守护线程
    private DestroyConnectionThread destroyConnectionThread;
    //具体执行类销毁功能，不止destroyConnectionThread，
    //还能交由ScheduledExecutorService执行
    private DestroyTask destroyTask;
    //连接池容器，volatile修饰确保添加与删除了元素在各个线程都可见
    private volatile DruidConnectionHolder[] connections;
    
    public DruidDataSource(boolean lockFair) {
        super(lockFair);
        //configFromPropety
    }
```
该类的核心功能就是：connections连接不够时唤醒createConnectionThread运行，而destroyConnectionThread则会定时运行。

## 初始化
```java
try {
   //有其它线程在初始化，则本次操作可中断
    try {
        lock.lockInterruptibly();
    } catch (InterruptedException e) {
        throw new SQLException("interrupt", e);
    }
    ……
    connections = new DruidConnectionHolder[maxActive];
    try {
        //init connections
        for (int i = 0, size = getInitialSize(); i < size; ++i) {
            PhysicalConnectionInfo pyConnectInfo = createPhysicalConnection();
            DruidConnectionHolder holder = new DruidConnectionHolder(this, pyConnectInfo);
            connections[poolingCount] = holder;

            incrementPoolingCount();
        }
		createAndStartCreatorThread();
	    createAndStartDestroyThread();
	    initedLatch.await();
	
	    init = true;
	    ……

    } catch (SQLException e) {
        LOG.error("init datasource error, url: " + jdbcUrl, e);
        connectError = e;
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
```
## 创建连接
创建连接在死循环中执行，间隔timeBetweenConnectErrorMillis秒创建一个连接。
总体包括4大部分：检测(会堵塞：empty.await())、创建、放入connections容器尾端(会发出信号：notEmpty.signal())、异常处理
## 销毁连接
因为关闭连接比较耗时，所以容器中销毁连接分为2大部分：

- 同步块中检测可销毁的连接并存入evictList；
- 非同步块将evictList中元素都销毁；

备注：检测是从头部开始，发现一个活跃的连接则退出检测，直接进入销毁部分。

## 获取连接
获取连接最终会调用takeLast()或pollLast(long nanos)，这两个方法的区别就是获取连接是否有等待时间。

```java
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
//                    BashUtils.executeJstack("dump.out");
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
```
takeLast()是获得了锁才会调用的。最后几行代码就是从容器中取出连接返回。