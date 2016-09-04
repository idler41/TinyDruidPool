package com.lfx.spring.aop.pattern.cglib;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/17 上午10:24.
 */
public class CglibFactory implements MethodInterceptor {

    private Object target;

    public Object getProxyInstance(Object target) {
        this.target = target;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(this);
        return enhancer.create();
    }

    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object result = null;

        System.out.println("前置增强");
        result = methodProxy.invokeSuper(o, args);
        System.out.println("后置增强");

        return result;
    }
}
