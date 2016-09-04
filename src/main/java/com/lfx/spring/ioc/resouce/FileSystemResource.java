package com.lfx.spring.ioc.resouce;

import com.lfx.spring.ioc.resouce.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/16 下午2:23.
 */
public class FileSystemResource implements Resource{

    private final File file;

    private final String path;

    public FileSystemResource(File file) {
        this.file = file;
        this.path = StringUtils.cleanPath(file.getPath());
    }

    public FileSystemResource(String path) {
        this.file = new File(path);
        this.path = StringUtils.cleanPath(path);
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }


    /**
     *
     * 文件系统的当前路径是系统根目录/，因为资源文件编译到target/classes/，
     * 所以路径为target/classes/123.txt
     *
     */
    public static void main(String[] args) throws IOException {
//        InputStream in = new FileSystemResource("/Users/apple/GitHub/MiniDruidPool/target/classes/123.txt").getInputStream();
//        File directory = new File("");
//        System.out.println(directory.getAbsolutePath());
        InputStream in = new FileSystemResource("target/classes/123.txt").getInputStream();

        byte[] b=new byte[in.available()];
        in.read(b);
        in.close();
        System.out.println(new String(b));
    }
}
