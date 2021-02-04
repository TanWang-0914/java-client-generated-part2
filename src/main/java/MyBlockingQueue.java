import javax.xml.crypto.Data;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class MyBlockingQueue {
    BlockingQueue<String[]> queue;
    private int count;

    public DataConsumer consumer;
    public MyBlockingQueue(int maxStoreID, int opHour, int numPurchases){
        count = maxStoreID * opHour * numPurchases;
        queue = new ArrayBlockingQueue<String[]>(count);
        consumer = new DataConsumer(queue, maxStoreID, count);
    }
}
