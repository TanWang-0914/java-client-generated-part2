import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MyBlockingQueue {
    BlockingQueue<String[]> queue;
    private int count;
    public volatile boolean finished = false;

    public DataConsumer consumer;
    public MyBlockingQueue(int maxStoreID, int opHour, int numPurchases){
        count = maxStoreID * opHour * numPurchases;
        queue = new ArrayBlockingQueue<String[]>(Math.max(20000, count/4));
        consumer = new DataConsumer(this, maxStoreID, count);
    }
}
