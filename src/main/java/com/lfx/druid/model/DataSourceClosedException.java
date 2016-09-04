package com.lfx.druid.model;

import java.sql.SQLException;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午5:42.
 */
public class DataSourceClosedException extends SQLException {

    private static final long serialVersionUID = 1L;

    public DataSourceClosedException(String reason){
        super(reason);
    }

}
