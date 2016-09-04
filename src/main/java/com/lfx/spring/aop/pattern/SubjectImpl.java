package com.lfx.spring.aop.pattern;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/17 上午10:09.
 */
public class SubjectImpl implements Subject {
    public void xxToEnhanced() {
        System.out.println("想要增强的接口方法");
    }

    public void xxToNotEnhanced() {
        System.out.println("不想要增强的接口方法");
    }

    public String specificMethod() {
        System.out.println("不想要增强的非接口方法");
        return "增强后获取返回结果";
    }
}
