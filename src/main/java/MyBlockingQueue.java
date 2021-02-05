import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MyBlockingQueue {
    BlockingQueue<String[]> queue;
    private int count;

    public DataConsumer consumer;
    public MyBlockingQueue(int maxStoreID, int opHour, int numPurchases){
        count = maxStoreID * opHour * numPurchases;
        queue = new ArrayBlockingQueue<String[]>(10000);
        consumer = new DataConsumer(queue, maxStoreID, count);
    }
}
