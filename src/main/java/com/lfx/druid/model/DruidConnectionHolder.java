package com.lfx.druid.model;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午2:13.
 */
public class DruidConnectionHolder {
    private final DruidAbstractDataSource dataSource;
    private final Connection connection;
    private final long connectTimeMillis;
    private transient long lastActiveTimeMillis;
    private final long createNanoSpan;
    private long useCount;

    public DruidConnectionHolder(DruidAbstractDataSource dataSource, DruidAbstractDataSource.PhysicalConnectionInfo pyConnectInfo) throws SQLException {
        this(dataSource, pyConnectInfo.getPhysicalConnection(), pyConnectInfo.getConnectNanoSpan());
    }

    public DruidConnectionHolder(DruidAbstractDataSource dataSource, Connection conn, long connectNanoSpan) throws SQLException {
        this.dataSource = dataSource;
        this.connection = conn;
        this.createNanoSpan = connectNanoSpan;
        this.connectTimeMillis = System.currentTimeMillis();
        this.lastActiveTimeMillis = connectTimeMillis;
    }

    public DruidAbstractDataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() {
        return connection;
    }

    public long getConnectTimeMillis() {
        return connectTimeMillis;
    }

    public long getLastActiveTimeMillis() {
        return lastActiveTimeMillis;
    }

    public void setLastActiveTimeMillis(long lastActiveTimeMillis) {
        this.lastActiveTimeMillis = lastActiveTimeMillis;
    }

    public long getCreateNanoSpan() {
        return createNanoSpan;
    }

    public long getUseCount() {
        return useCount;
    }

    public void incrementUseCount() {
        useCount++;
    }
}
