package com.lfx.multithread.case0;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/19 下午2:39.
 */
public class Storage {

    private int maxPoolSize = 60;

    private ArrayBlockingQueue<Product> blockingPool = new ArrayBlockingQueue<Product>(maxPoolSize);


    public void put(Product product) {
        try {
            long waitNanos = System.nanoTime();
            blockingPool.put(product);
            waitNanos = System.nanoTime() - waitNanos;
            System.out.println("put消耗时间：" + waitNanos);
        } catch (InterruptedException e) {
            //异常处理
        }
    }

    public Product poll() {
//        Product product = null;
//        try {
        long waitNanos = System.nanoTime();

        Product product = blockingPool.poll();
        if (product != null) {
            waitNanos = System.nanoTime() - waitNanos;
            System.out.println("poll消耗时间：" + waitNanos);
        }
        return blockingPool.poll();

//        } catch (InterruptedException e) {
//            //exception handler
//        }
//        return product;
    }

    public boolean isEmpty() {
        return blockingPool.isEmpty();
    }

}
