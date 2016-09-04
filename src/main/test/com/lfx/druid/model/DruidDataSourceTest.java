package com.lfx.druid.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/14 下午9:55.
 */
public class DruidDataSourceTest {

    private static final String driverClassName = "com.mysql.jdbc.Driver";
    private static final String url = "jdbc:mysql://192.168.3.6:3306/test?useUnicode=true&characterEncoding=UTF-8";
    private static final String username = "root";
    private static final String password = "123456";


    public static void main(String[] args) {
//        Properties properties = new Properties();
//        properties.put("driverClassName", "com.mysql.jdbc.Driver");
//        properties.put("url", "jdbc:mysql://192.168.3.6:3306/test?useUnicode=true&characterEncoding=UTF-8");
//        properties.put("username", "root");
//        properties.put("password", "123456");

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClass(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
//        dataSource.setConnectProperties(properties);

        try {
            DruidPooledConnection conn = dataSource.getConnection();
            if (conn != null) {
                System.out.println("获取连接：" + conn);
            } else {
                System.out.println("conn == null");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        dataSource.close();
    }
}
