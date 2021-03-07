import com.opencsv.CSVReader;
import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.PurchaseApi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class PurchaseApiTest {
    private static Phase phase = new Phase();
    private static ReqCount reqCount;

    public static void main(String[] args) {

//        PurchaseApi apiInstance = new PurchaseApi();
//        apiInstance.getApiClient().setBasePath("http://localhost:8080/HW_1_war_exploded/");
//        apiInstance.getApiClient().setBasePath("http://52.91.146.150:8080/HW_1_war/");
//        String basePath = "http://localhost:8080/HW_1_war_exploded/";
//        String basePath = "http://54.144.146.250:8080/HW_1_war/";
        String basePath = "http://CS6650LoadBalancer-771575157.us-east-1.elb.amazonaws.com:8080/HW_1_war/";


        int maxStores=1, maxCustID, maxItemID, numPurchases, numItemPerPurchase, date;
        String ipAddress = "0.0.0.0";
        Scanner scanner = new Scanner(System.in);
        // read maxStores
        System.out.println("Enter maximum number of stores to simulate (maxStores):");
        while (scanner.hasNextInt()){
            int input = scanner.nextInt();
            if (input > 0){
                maxStores = input;
                System.out.println("maxStores = "+maxStores);
                break;
            }else{
                System.out.println("You entered a negative integer, try again. ");
            }
        }

        //read maxCustID
        System.out.println("Enter number of customers/store (default 1000):");
        if (scanner.hasNextInt()){
            int input = scanner.nextInt();
            if (input > 0){
                maxCustID = input;
                System.out.println("maxCustID = "+maxCustID);
            }else{
                maxCustID = 1000;
                System.out.println("maxCustID = 1000 ");
            }
        }else{
            maxCustID = 1000;
            System.out.println("maxCustID = 1000 ");
        }

        //read maxItemID
        System.out.println("Enter maximum itemID - default 100000:");
        if (scanner.hasNextInt()){
            int input = scanner.nextInt();
            if (input > 0){
                maxItemID = input;
                System.out.println("maxItemID = "+maxItemID);
            }else{
                maxItemID = 10000;
                System.out.println("maxItemID = 100000");
            }
        }else{
            maxItemID = 10000;
            System.out.println("maxItemID = 100000");
        }

        //read numPurchases
        System.out.println("Enter number of purchases per hour: (default 60):");
        if (scanner.hasNextInt()){
            int input = scanner.nextInt();
            if (input > 0 ){
                numPurchases = input;
                System.out.println("numPurchases = "+numPurchases);
            }else{
                numPurchases = 60;
                System.out.println("numPurchases = 60");
            }
        }else{
            numPurchases = 60;
            System.out.println("numPurchases = 60");
        }

        //read numItemPerPurchase
        System.out.println("Enter number of items for each purchase (range 1-20, default 5):");
        if (scanner.hasNextInt()){
            int input = scanner.nextInt();
            if (input > 0 && input <= 20){
                numItemPerPurchase = input;
                System.out.println("numItemPerPurchase = "+numItemPerPurchase);
            }else{
                numItemPerPurchase = 5;
                System.out.println("numItemPerPurchase = 5");
            }
        }else{
            numItemPerPurchase = 5;
            System.out.println("numItemPerPurchase = 5");
        }

        //read date
        System.out.println("Enter date - default to 20210101:");
        if (scanner.hasNextInt()){
            int input = scanner.nextInt();
            if (input >= 20210101 && input <= 21210101){
                date = input;
                System.out.println("date = "+date);
            }else{
                date = 20210101;
                System.out.println("date = 20210101");
            }
        }else{
            date = 2021010;
            System.out.println("date = 20210101");
        }

        scanner.nextLine();

        //read ipAddress
        System.out.println("Enter ipAddress :");
        if (scanner.hasNextLine()){
            ipAddress = scanner.nextLine();
        }

        // Thread Array to store each Store Thread
        int currentStoreIndex = 0;
        Thread[] storeThreads = new Thread[maxStores];
        // setting operating hour to 9 hours per day
        int opHours = 9;

        // create request count class
        reqCount = new ReqCount(maxStores,opHours);

        // create blockingQueue and dataConsumer, and start dataConsumer thread
        MyBlockingQueue blockingQueue = new MyBlockingQueue(maxStores, opHours, numPurchases);
        Thread consumerThread = new Thread(blockingQueue.consumer);
        consumerThread.start();

        // read start time stamp
        long startTime = System.currentTimeMillis();

        // Start running
        System.out.println("EastPhaseStart");
        CountDownLatch centralPhaseLatch = new CountDownLatch(1);
        CountDownLatch westPhaseLatch = new CountDownLatch(1);
        for (; currentStoreIndex < maxStores/4; currentStoreIndex++){
            RequestsPerStore storeThread = new RequestsPerStore(currentStoreIndex, maxCustID, maxItemID,numPurchases,numItemPerPurchase,String.valueOf(date),opHours,basePath,centralPhaseLatch, westPhaseLatch, reqCount, blockingQueue.queue);
            storeThreads[currentStoreIndex] = new Thread(storeThread);
            storeThreads[currentStoreIndex].start();
        }

        try {
            centralPhaseLatch.await();
            System.out.println("CentralPhaseStart");
            for (; currentStoreIndex < maxStores/2; currentStoreIndex++){
                RequestsPerStore storeThread = new RequestsPerStore(currentStoreIndex,maxCustID,maxItemID,numPurchases,numItemPerPurchase,String.valueOf(date),opHours,basePath,centralPhaseLatch,westPhaseLatch,reqCount,blockingQueue.queue);
                storeThreads[currentStoreIndex] = new Thread(storeThread);
                storeThreads[currentStoreIndex].start();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            westPhaseLatch.await();
            System.out.println("WestPhaseStart");
            for (; currentStoreIndex < maxStores; currentStoreIndex++){
                RequestsPerStore storeThread = new RequestsPerStore(currentStoreIndex,maxCustID,maxItemID,numPurchases,numItemPerPurchase,String.valueOf(date),opHours,basePath,centralPhaseLatch,westPhaseLatch,reqCount,blockingQueue.queue);
                storeThreads[currentStoreIndex] = new Thread(storeThread);
                storeThreads[currentStoreIndex].start();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // wait for each one to finish
        try {
            for (int i = 0; i < maxStores; i++) {
                storeThreads[i].join();
            }
        }catch (InterruptedException e){
            System.out.println(e.getMessage());
        }

        blockingQueue.finished = true;

        long endTime = System.currentTimeMillis();

        int totalSucReq = reqCount.succReq;
        int totalFailReq = maxStores*opHours*numPurchases-totalSucReq;

        double timePeriod = ((endTime-startTime)/1000.0);
        double throughput = (totalSucReq + totalFailReq)/ timePeriod;

        System.out.println("Number of Stores/threads:" + maxStores);
        System.out.println("All threads finished");
        System.out.println("Total successful Request:" + totalSucReq);
        System.out.println("Total failed Request:" + totalFailReq);
        System.out.println("Time Period:" + timePeriod);
        System.out.println("Throughput:" + throughput);


        // close consumer thread, this step will make sure all records are wrote to CSV File
        try{
            consumerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // read response records and calculate response time
        long totalReqTime = 0;
        List<Integer> responseTimeList = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader("./"+maxStores + "part2.csv"))){
            String[] line;
            while ((line = reader.readNext()) != null){
                if (line.length == 4 && line[3].equals("201")){
                    int resTime = Integer.parseInt(line[2]);
                    totalReqTime += resTime;
                    responseTimeList.add(resTime);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(responseTimeList);
        int recordsLen = responseTimeList.size();
        long meanResTime = totalReqTime/recordsLen;
        int medianResTime = responseTimeList.get(recordsLen/2);
        int p99ResTime = responseTimeList.get((int)(recordsLen*0.99));
        int maxResTime = responseTimeList.get(responseTimeList.size()-1);

        System.out.println("Mean Response Time:" + meanResTime);
        System.out.println("Median Response Time:" + medianResTime);
        System.out.println("P99 Response Time:" + p99ResTime);
        System.out.println("Max Response Time:" + maxResTime);

        System.out.println("consumer thread finished.");
        System.out.println("Program finished.");

    }


}
