package com.lfx.multithread.case0;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/19 下午2:38.
 */
public class Producer extends Thread {

    private Storage storage;
    private CountDownLatch startLatch;
    private AtomicInteger productCount;

    public Producer(Storage storage, CountDownLatch startLatch, AtomicInteger productCount) {
        this.productCount = productCount;
        this.storage = storage;
        this.startLatch = startLatch;
    }

    @Override
    public void run() {
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            //异常处理
            e.printStackTrace();
        }
        for (int i = 0; i < 1; i++) {
            storage.put(new Product());
            productCount.incrementAndGet();
        }
    }
}