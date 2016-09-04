package com.lfx.multithread.case0;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/19 下午2:37.
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer extends Thread {

    private Storage storage;
    private CountDownLatch startLatch;
    private CountDownLatch consumerLatch;

    private AtomicInteger consumeCount;

    public Consumer(Storage storage, CountDownLatch startLatch,
                    CountDownLatch consumerLatch, AtomicInteger consumeCount) {
        this.storage = storage;
        this.startLatch = startLatch;
        this.consumerLatch = consumerLatch;
        this.consumeCount = consumeCount;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            startLatch.await();

            for (; ; ) {

                Product product = storage.poll();
                if (product != null) {
                    consumerLatch.countDown();
                    consumeCount.incrementAndGet();
                }
            }
        } catch (InterruptedException e) {
            //异常处理
        }
    }
}
