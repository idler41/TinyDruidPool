package com.lfx.multithread.case0;

import org.junit.Test;

import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 场景模拟：M个线程同时往队列中存入一个消息，N个消费者从队列中消费一个消息。
 * 测试结果：每个线程平均存入队列中耗费时间。
 *
 * @Author idler [idler41@163.com]
 * @Date 16/8/18 下午10:27.
 */
public class BlockingQueueCase {

    private int producerNums = 500;
    private int poolSize = 50;
    private int consumerNums = 5;

    private String msg = "hello world!!!";

    private CountDownLatch initedLatch = new CountDownLatch(producerNums + consumerNums);
    private CountDownLatch endLatch = new CountDownLatch(producerNums);
    private AtomicLong timeCount = new AtomicLong();

    private BlockingQueue<String> blockingQueue;

    @Test
    public void ArrayBlockingTest() throws InterruptedException {
        //平均耗时：71,842 ns
        countPutTimeTest(new ArrayBlockingQueue<String>(poolSize));
    }

    @Test
    public void LinkedBlockingTest() throws InterruptedException {
        //平均耗时：112,199 ns
        countPutTimeTest(new LinkedBlockingQueue<String>(poolSize));
    }


    private void countPutTimeTest(BlockingQueue<String> blockingQueue) throws InterruptedException {
        this.blockingQueue = blockingQueue;
        for (int i = 0; i < producerNums; i++) {
            new Producer().start();
            initedLatch.countDown();
        }
        for (int i = 0; i < consumerNums; i++) {
            new Consumer().start();
            initedLatch.countDown();
        }
        endLatch.await();
        System.out.println("put平均耗时：" + new DecimalFormat("#,###")
                .format((float) timeCount.get() / producerNums) + " ns");
    }

    class Producer extends Thread {
        @Override
        public void run() {
            try {
                initedLatch.await();
                long waitTime = System.nanoTime();
                blockingQueue.put(msg);
                waitTime = System.nanoTime() - waitTime;
                System.out.println(getName() + " put()耗时：" + waitTime + "ns");
                timeCount.getAndAdd(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        }
    }

    class Consumer extends Thread {

        public Consumer() {
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                initedLatch.await();

                for (; ; ) {
                    blockingQueue.take();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

