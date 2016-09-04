package com.lfx.druid.model;

import java.sql.SQLException;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午6:05.
 */
public class DataSourceNotAvailableException extends SQLException {

    private static final long serialVersionUID = 1L;

    public DataSourceNotAvailableException(Throwable cause){
        super(cause);
    }

}