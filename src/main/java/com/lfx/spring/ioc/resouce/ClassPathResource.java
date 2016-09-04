package com.lfx.spring.ioc.resouce;

import com.lfx.spring.ioc.resouce.utils.ClassUtils;
import com.lfx.spring.ioc.resouce.utils.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/16 下午2:23.
 */
public class ClassPathResource implements Resource{

    private final String path;

    private ClassLoader classLoader;

    private Class<?> clazz;

    public ClassPathResource(String path) {
        this(path, (ClassLoader) null);
    }

    public ClassPathResource(String path, ClassLoader classLoader) {
        String pathToUse = StringUtils.cleanPath(path);
        if (pathToUse.startsWith("/")) {
            pathToUse = pathToUse.substring(1);
        }
        this.path = pathToUse;
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }

    public ClassPathResource(String path, Class<?> clazz) {
        this.path = StringUtils.cleanPath(path);
        this.clazz = clazz;
    }

    public ClassPathResource(String path, ClassLoader classLoader, Class<?> clazz) {
        this.path = StringUtils.cleanPath(path);
        this.classLoader = classLoader;
        this.clazz = clazz;
    }

    public InputStream getInputStream() throws IOException {
        InputStream is;
        if (this.clazz != null) {
            is = this.clazz.getResourceAsStream(this.path);
        }else if (this.classLoader != null) {
            is = this.classLoader.getResourceAsStream(this.path);
        } else {
            is = ClassLoader.getSystemResourceAsStream(this.path);
        }

        if (is == null) {
            throw new FileNotFoundException(this.path + "cannot be opened because it does not exist");
        }

        return is;
    }

    /*
    * ClassLoader加载路径为编译路径target/Classes/
    * resource资源文件夹会编译到target/Classes/所以resource/123.txt可以找到
    * */
    public static void main(String[] args) throws IOException {
//        System.out.println(System.getProperty("java.class.path"));
        InputStream in = new ClassPathResource("123.txt").getInputStream();
//        InputStream in = new ClassPathResource("123.txt",ClassPathResource.class).getInputStream();

        byte[] b=new byte[in.available()];
        in.read(b);
        in.close();
        System.out.println(new String(b));
    }
}
