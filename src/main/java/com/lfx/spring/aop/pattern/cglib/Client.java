package com.lfx.spring.aop.pattern.cglib;

import com.lfx.spring.aop.pattern.SubjectImpl;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/17 上午10:39.
 */
public class Client {
    public static void main(String[] args) {
        CglibFactory cglib = new CglibFactory();
        SubjectImpl subjectImpl = (SubjectImpl) cglib.getProxyInstance(new SubjectImpl());

        subjectImpl.xxToEnhanced();
        System.out.println();

        subjectImpl.xxToNotEnhanced();
        System.out.println();

        System.out.println(subjectImpl.specificMethod());
    }
}
