import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单服务器竞拍
 * @Author idler [idler41@163.com]
 * @Date 16/8/12 上午4:45.
 */
public class XxxAuctionService {
    private volatile int maxPrice;

    private Lock lock;

    //spring注入, 其它业务也需要调度服务，所以统一管理
    private ScheduledExecutorService pushScheduler;
    private AndriodPushMaxPriceTask andriodPush;
    private IOSPushMaxPriceTask iosPush;
    private WebPushMaxPriceTask webPush;

    private int scheduleMillis = 1000;

    public XxxAuctionService() {
        lock = new ReentrantLock(false);
    }

    //TODO 高并发下会出现线程频繁的进行核心态与用户态的切换
    public void bid(int price) {
        if (price <= maxPrice) {
            return;
        }
        try {
            lock.lock();

            if (price > maxPrice) {
                maxPrice = price;
            }
        } finally {
            lock.unlock();
        }

    }

    public void startBid() {
        pushScheduler.schedule(new AndriodPushMaxPriceTask(),
                scheduleMillis, TimeUnit.MILLISECONDS);
    }

    public void stopBid() {
//        pushScheduler.s
    }

    public int getMaxPrice() {
        //竞拍开始时，客户端定时获取最高价
        return maxPrice;
    }

    public class AndriodPushMaxPriceTask implements Runnable {

        public void run() {
            //客户端推送
        }
    }

    public class IOSPushMaxPriceTask implements Runnable {

        public void run() {
            //客户端推送
        }
    }

    public class WebPushMaxPriceTask implements Runnable {

        public void run() {
            //客户端推送
        }
    }
}
