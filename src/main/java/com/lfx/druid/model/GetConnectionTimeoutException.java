package com.lfx.druid.model;

import java.sql.SQLException;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/13 下午5:37.
 */
public class GetConnectionTimeoutException extends SQLException {
    private static final long serialVersionUID = 1L;

    public GetConnectionTimeoutException(String reason) {
        super(reason);
    }

    public GetConnectionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
