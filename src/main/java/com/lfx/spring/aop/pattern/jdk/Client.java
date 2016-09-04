package com.lfx.spring.aop.pattern.jdk;

import com.lfx.spring.aop.pattern.SubjectImpl;
import com.lfx.spring.aop.pattern.Subject;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/17 上午10:17.
 */
public class Client {
    public static void main(String[] args) {
        SubjectImpl specificService = new SubjectImpl();
        Subject subject = (Subject) new ProxyFactory().getProxyInstance(specificService);

        subject.xxToEnhanced();
        System.out.println("");
        subject.xxToNotEnhanced();
    }
}
