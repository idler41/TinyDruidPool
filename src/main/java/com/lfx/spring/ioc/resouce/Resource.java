package com.lfx.spring.ioc.resouce;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/16 下午2:23.
 */
public interface Resource {

    InputStream getInputStream() throws IOException;
}
