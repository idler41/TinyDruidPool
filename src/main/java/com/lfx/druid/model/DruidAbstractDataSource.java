package com.lfx.druid.model;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午2:14.
 */

public class DruidAbstractDataSource {

    protected volatile boolean inited = false;
    protected Date initedTime;
    protected boolean failFast = false;
    protected int connectionErrorRetryAttempts = 30;
    protected boolean breakAfterAcquireFailure = false;

    protected AtomicLong createCount = new AtomicLong();
    protected AtomicLong destroyCount = new AtomicLong();

    protected volatile int maxWaitThreadCount = -1;

    protected long createTimespan;

    protected volatile Throwable createError;
//    protected volatile boolean removeAbandoned;

    protected ReentrantLock lock;
    protected Condition notEmpty;
    protected Condition empty;

    //为什么不用volatile boolean,代码中没有发现复合操作
    protected AtomicBoolean failContinuous = new AtomicBoolean(false);
    protected int notFullTimeoutRetryCount = 0;

    protected volatile int initialSize = DEFAULT_INITIAL_SIZE;
    protected volatile int maxActive = DEFAULT_MAX_ACTIVE_SIZE;
    protected volatile int minIdle = DEFAULT_MIN_IDLE;
    protected volatile long maxWait = DEFAULT_MAX_WAIT;
    protected volatile long phyTimeoutMillis = DEFAULT_PHY_TIMEOUT_MILLIS;

    //最小生存时间
    protected volatile long minEvictableIdleTimeMillis = DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    protected volatile long maxEvictableIdleTimeMillis = DEFAULT_MAX_EVICTABLE_IDLE_TIME_MILLIS;
    protected volatile long timeBetweenEvictionRunsMillis = DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
    protected volatile long timeBetweenConnectErrorMillis = DEFAULT_TIME_BETWEEN_CONNECT_ERROR_MILLIS;

    public final static int DEFAULT_INITIAL_SIZE = 0;
    public final static int DEFAULT_MAX_ACTIVE_SIZE = 8;
    public final static int DEFAULT_MIN_IDLE = 0;
    public final static int DEFAULT_MAX_WAIT = -1;
    public static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS = 1000L * 60L * 30L;
    public static final long DEFAULT_MAX_EVICTABLE_IDLE_TIME_MILLIS = 1000L * 60L * 60L * 7;
    public static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS = 60 * 1000L;
    public static final long DEFAULT_PHY_TIMEOUT_MILLIS = -1;
    public static final long DEFAULT_TIME_BETWEEN_CONNECT_ERROR_MILLIS = 30 * 1000;

    protected volatile String username;
    protected volatile String password;
    protected volatile String jdbcUrl;
    protected volatile String driverClass;
    protected volatile Properties connectProperties;
    protected Driver driver;
    protected volatile ClassLoader driverClassLoader;


    public DruidAbstractDataSource(boolean lockFair) {
        this.lock = new ReentrantLock((lockFair));
        this.notEmpty = lock.newCondition();
        this.empty = lock.newCondition();
    }

    /**
     * 创建连接信息包括三部分：实际的物理连接；连接用户名、密码等；创建连接各阶段花费时间
     *
     * @return
     * @throws SQLException
     */
    public PhysicalConnectionInfo createPhysicalConnection() throws SQLException {
        String url = this.getJdbcUrl();
        String user = getUsername();
        String password = getPassword();

        Properties physicalConnectProperties = new Properties();
        if (connectProperties != null) {
            physicalConnectProperties.putAll(connectProperties);
        }

        if (user != null && user.length() != 0) {
            physicalConnectProperties.put("user", user);
        }

        if (password != null && password.length() != 0) {
            physicalConnectProperties.put("password", password);
        }

        Connection conn;
        long connectStartNanos = System.nanoTime();
        long connectedNanos, initedNanos, validatedNanos;
        try {
            conn = createPhysicalConnection(url, physicalConnectProperties);
            connectedNanos = System.nanoTime();

            initPhysicalConnection(conn);
            initedNanos = System.nanoTime();

            validateConnection(conn);
            validatedNanos = System.nanoTime();

            if (conn == null) {
                throw new SQLException("connect error, url " + url + ", driverClass " + this.driverClass);
            }

        } catch (SQLException e) {
            throw e;
        } finally {
            long nano = System.nanoTime() - connectStartNanos;
            createTimespan += nano;
        }
        return new PhysicalConnectionInfo(conn, connectStartNanos, connectedNanos, initedNanos, validatedNanos);
    }

    public Connection createPhysicalConnection(String url, Properties info) throws SQLException {
        Connection conn = getDriver().connect(url, info);

        createCount.incrementAndGet();

        return conn;
    }

    public void initPhysicalConnection(Connection conn) throws SQLException {
        //设置Connection属性,ReadOnly、事务隔离级别等
    }

    public void validateConnection(Connection conn) throws SQLException {
        //检测连接是否有效
    }

    public static class PhysicalConnectionInfo {
        private Connection connection;
        private long initedNanos;
        private long connectStartNanos;
        private long connectedNanos;
        private long validatedNanos;

        public PhysicalConnectionInfo(Connection connection,
                                      long connectStartNanos,
                                      long connectedNanos,
                                      long initedNanos,
                                      long validatedNanos) {
            this.connection = connection;
            this.initedNanos = initedNanos;
            this.connectStartNanos = connectStartNanos;
            this.connectedNanos = connectedNanos;
            this.validatedNanos = validatedNanos;
        }

        public Connection getPhysicalConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public long getConnectStartNanos() {
            return connectStartNanos;
        }

        public void setConnectStartNanos(long connectStartNanos) {
            this.connectStartNanos = connectStartNanos;
        }

        public long getConnectedNanos() {
            return connectedNanos;
        }

        public void setConnectedNanos(long connectedNanos) {
            this.connectedNanos = connectedNanos;
        }

        public long getInitedNanos() {
            return initedNanos;
        }

        public long getValidatedNanos() {
            return validatedNanos;
        }

        public long getConnectNanoSpan() {
            return connectedNanos - connectStartNanos;
        }
    }


    public boolean isInited() {
        return inited;
    }

    public void setInited(boolean inited) {
        this.inited = inited;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public long getMaxEvictableIdleTimeMillis() {
        return maxEvictableIdleTimeMillis;
    }

    public void setMaxEvictableIdleTimeMillis(long maxEvictableIdleTimeMillis) {
        this.maxEvictableIdleTimeMillis = maxEvictableIdleTimeMillis;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Properties getConnectProperties() {
        return connectProperties;
    }

    public void setConnectProperties(Properties connectProperties) {
        this.connectProperties = connectProperties;
    }

    public int getMaxWaitThreadCount() {
        return maxWaitThreadCount;
    }

    public void setMaxWaitThreadCount(int maxWaitThreadCount) {
        this.maxWaitThreadCount = maxWaitThreadCount;
    }
}
