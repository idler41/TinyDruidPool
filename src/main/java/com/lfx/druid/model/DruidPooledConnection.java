package com.lfx.druid.model;

import java.sql.Connection;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午5:33.
 */
public class DruidPooledConnection {
    protected Connection conn;
    protected volatile DruidConnectionHolder holder;
    private final Thread ownerThread;
    private long connectedTimeMillis;

    public DruidPooledConnection(DruidConnectionHolder holder) {

        this.conn = holder.getConnection();
        this.holder = holder;
        ownerThread = Thread.currentThread();
        connectedTimeMillis = System.currentTimeMillis();
    }
}
