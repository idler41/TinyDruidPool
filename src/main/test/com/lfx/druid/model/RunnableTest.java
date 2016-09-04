package com.lfx.druid.model;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/16 上午12:17.
 */
public class RunnableTest {

    private ThreadA threadA;
    private ThreadB threadB;
    private RunnableC runnableC;

    public RunnableTest() {
        threadA = new ThreadA("threadA");
        threadB = new ThreadB("threadB");
        runnableC = new RunnableC();
    }

    public void start() {
        threadA.start();
    }

    public static void main(String[] args) {
        new RunnableTest().start();
    }

    public class ThreadA extends Thread {
        public ThreadA(String name) {
            super(name);
        }

        @Override
        public void run() {
            System.out.println("ThreadA.run(),currentThread:" + Thread.currentThread().getName());
            threadB.start();
        }
    }

    public class ThreadB extends Thread{
        public ThreadB(String name) {
            super(name);
        }

        @Override
        public void run() {
            System.out.println("ThreadB.run(),currentThread:" + Thread.currentThread().getName());
            runnableC.run();
        }
    }

    public class RunnableC implements Runnable {

        public void run() {
            System.out.println("RunnableC.run(),currentThread:" + Thread.currentThread().getName());
        }
    }
}
