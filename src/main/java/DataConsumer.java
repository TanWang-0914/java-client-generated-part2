import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class DataConsumer implements Runnable{

    BlockingQueue<String[]> queue;
    private int maxStoreID;
    private int count;

    public DataConsumer(BlockingQueue<String[]> queue, int maxStoreID, int count){
        this.queue = queue;
        this.maxStoreID = maxStoreID;
        this.count = count;
    }

    @Override
    public void run() {
        File csvFile = new File("./" + maxStoreID + "part2.csv");
        try {
            FileWriter outputFile = new FileWriter(csvFile);
            CSVWriter writer = new CSVWriter(outputFile);
            String[] header = {"startTime", "requestType","latency","responseCode"};
            writer.writeNext(header);
            while (count>0){
                String[] data = queue.take();
                writer.writeNext(data);
                count--;
            }
            writer.flush();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
