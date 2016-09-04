package com.lfx.druid.model;

import java.sql.SQLException;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午5:43.
 */
public class DataSourceDisableException extends SQLException {

    private static final long serialVersionUID = 1L;

    public DataSourceDisableException(){
        super();
    }

    public DataSourceDisableException(String reason){
        super(reason);
    }

    public DataSourceDisableException(Throwable cause){
        super(cause);
    }

}