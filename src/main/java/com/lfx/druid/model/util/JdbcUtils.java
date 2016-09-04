package com.lfx.druid.model.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/10 上午4:17.
 */
public class JdbcUtils {
    private final static Logger log = LoggerFactory.getLogger(JdbcUtils.class);

    public static void close(Statement x) {
        if (x == null) {
            return;
        }
        try {
            x.close();
        } catch (SQLException e) {
            log.debug("close statement set error,", e);
        }
    }

    public static void close(ResultSet x) {
        if (x == null) {
            return;
        }
        try {
            x.close();
        } catch (SQLException e) {
            log.debug("close result set error,", e);
        }
    }

    public static void close(Connection x) {
        if (x == null) {
            return;
        }
        try {
            x.close();
        } catch (Exception e) {
            log.debug("close connection error", e);
        }
    }

    public static Driver createDriver(ClassLoader classLoader, String driverClassName) throws SQLException {
        Class<?> clazz = null;
        if (classLoader != null) {
            try {
                clazz = classLoader.loadClass(driverClassName);
            } catch (ClassNotFoundException e) {
                // skip
            }
        }

        if (clazz == null) {
            try {
                ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
                if (contextLoader != null) {
                    clazz = contextLoader.loadClass(driverClassName);
                }
            } catch (ClassNotFoundException e) {
                // skip
            }
        }

        if (clazz == null) {
            try {
                clazz = Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.getMessage(), e);
            }
        }

        try {
            return (Driver) clazz.newInstance();
        } catch (IllegalAccessException e) {
            throw new SQLException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
