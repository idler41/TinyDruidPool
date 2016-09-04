package com.lfx.reference;

/**
 * 测试类用于：
 * 1、根搜索算法是否解决对象互相引用问题
 * 2、GC日志分析对象生存周期
 * @Author idler [idler41@163.com]
 * @Date 16/8/18 下午4:09.
 */
public class RootTracingTest {

    /**
     * jvm启动添加参数-XX:+PrintHeapAtGC -Xloggc:./logs/root_tracing_gc.log
     */
    public static void main(String[] args) throws InterruptedException {
        Test objA = new Test();
        Test objB = new Test();

        //A引用B
        objA.instance = objB;
        //B引用A
        objB.instance = objA;

//        BashUtils.executeJmap();
//        System.out.println("gc前");
        objA = null;
        objB = null;

        System.gc();
        for (int i = 0; i < 10; i++ ) {
            Thread.sleep(100);
        }

        //分析：
        // 1.发生了一次YoungGC、一次FullGC，对象已经回收，解决了对象循环引用问题。
        // 2.
    }
}

class Test {
    public Test instance;

    private static final int _100MB = 100 * 1024 * 1024;

    //占用多点内存，使得GC日志中能看清楚是否被回收
    //由于要在内存中开辟一块较大的连续区域，它直接被分配在老年代(ParOldGen)中
    private byte[] bigSize = new byte[2 * _100MB];
}
