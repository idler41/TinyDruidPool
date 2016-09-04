package com.lfx.spring.aop.pattern.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/17 上午10:11.
 */
public class ProxyFactory implements InvocationHandler{

    private Object target;

    public Object getProxyInstance(Object target) {
        this.target = target;
        Class targetClazz = target.getClass();
        ClassLoader targetClassLoader = targetClazz.getClassLoader();
        Class<?>[] interfaces = targetClazz.getInterfaces();
        return Proxy.newProxyInstance(targetClassLoader, interfaces, this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;

        System.out.println("前置增强");
        result = method.invoke(target, args);
        System.out.println("后置增强");

        return result;
    }
}
