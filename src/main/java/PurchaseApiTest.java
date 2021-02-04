import com.opencsv.CSVReader;
import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.PurchaseApi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PurchaseApiTest {
    private static Phase phase = new Phase();
    private static ReqCount reqCount;

    public static void main(String[] args) {

        PurchaseApi apiInstance = new PurchaseApi();
        apiInstance.getApiClient().setBasePath("http://localhost:8080/HW_1_war_exploded/");
        System.out.println(apiInstance.getApiClient().getBasePath());

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
            date = 20210101;
            System.out.println("date = 20210101");
        }

        scanner.nextLine();

        //read ipAddress
        System.out.println("Enter ipAddress :");
        if (scanner.hasNextLine()){
            ipAddress = scanner.nextLine();
        }

        int currentStoreIndex = 0;
        Thread[] storeThreads = new Thread[maxStores];
        int opHour = 12;

        reqCount = new ReqCount(maxStores,opHour);

        MyBlockingQueue blockingQueue = new MyBlockingQueue(maxStores, opHour, numPurchases);
        Thread consumerThread = new Thread(blockingQueue.consumer);
        consumerThread.start();

        long startTime = System.currentTimeMillis();

        while (true){
            if (phase.getPhase().equals("EastPhaseStart")){
                System.out.println(phase.currentPhase);
                phase.changePhase("EastPhaseRunning");
                for (; currentStoreIndex < maxStores/4; currentStoreIndex++){
                    RequestsPerStore storeThread = new RequestsPerStore(currentStoreIndex,maxCustID,maxItemID,numPurchases,numItemPerPurchase,String.valueOf(date),apiInstance,phase,reqCount,blockingQueue.queue);
                    storeThreads[currentStoreIndex] = new Thread(storeThread);
                    storeThreads[currentStoreIndex].start();
                }
            }
            else if (phase.getPhase().equals("CentralPhaseStart")){
//                System.out.println(phase.currentPhase);
                phase.changePhase("CentralPhaseRunning");
                for (; currentStoreIndex < maxStores/2; currentStoreIndex++){
                    RequestsPerStore storeThread = new RequestsPerStore(currentStoreIndex,maxCustID,maxItemID,numPurchases,numItemPerPurchase,String.valueOf(date),apiInstance,phase,reqCount, blockingQueue.queue);
                    storeThreads[currentStoreIndex] = new Thread(storeThread);
                    storeThreads[currentStoreIndex].start();
                }
            }
            else if (phase.getPhase().equals("WestPhaseStart")){
//                System.out.println(phase.currentPhase);
                phase.changePhase("WestPhaseRunning");
                for (; currentStoreIndex < maxStores; currentStoreIndex++){
                    RequestsPerStore storeThread = new RequestsPerStore(currentStoreIndex,maxCustID,maxItemID,numPurchases,numItemPerPurchase,String.valueOf(date),apiInstance,phase,reqCount, blockingQueue.queue);
                    storeThreads[currentStoreIndex] = new Thread(storeThread);
                    storeThreads[currentStoreIndex].start();
                }
                break;
            }else{
                continue;
            }
        }

        // wait for each one to finish
        try {
            for (int i = 0; i < maxStores; i++) {
                storeThreads[i].join();
            }
        }catch (InterruptedException e){
            System.out.println(e.getMessage());
        }

        long endTime = System.currentTimeMillis();

        int totalSucReq = 0;
        int totalFailReq = 0;

        for (int storeId = 0; storeId < maxStores; storeId++){
            int numSuc = 0;
            int numFail = 0;
            for(int ns: reqCount.successfulReq[storeId]) numSuc += ns;
            for(int nf: reqCount.failedReq[storeId]) numFail += nf;
            //System.out.println("Store" + storeId + ": suc request " + numSuc + ", fail request" + numFail);
            totalSucReq += numSuc;
            totalFailReq += numFail;
        }

        double timePeriod = ((endTime-startTime)/1000.0);
        double throughput = (totalSucReq + totalFailReq)/ timePeriod;

        System.out.println("Number of Stores/threads:" + maxStores);
        System.out.println("All threads finished");
        System.out.println("Total successful Request:" + totalSucReq);
        System.out.println("Total failed Request:" + totalFailReq);
        System.out.println("Time Period:" + timePeriod);
        System.out.println("Throughput:" + throughput);



        try{
            consumerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

        System.out.println("Mean Response Time:" + meanResTime);
        System.out.println("Median Response Time:" + medianResTime);
        System.out.println("P99 Response Time:" + p99ResTime);

        System.out.println("consumer thread finished.");


//        PurchaseItems testItem1 = new PurchaseItems();
//        testItem1.setItemID("10001");
//        testItem1.setNumberOfItems(1);
//        System.out.println(testItem1.toString());
//        PurchaseItems testItem2 = new PurchaseItems();
//        testItem2.setItemID("10002");
//        testItem2.setNumberOfItems(1);
//        System.out.println(testItem2.toString());
//        Purchase body = new Purchase(); // Purchase | items purchased
//        body.addItemsItem(testItem1);
//        body.addItemsItem(testItem2);
//
//        Integer storeID = 56; // Integer | ID of the store the purchase takes place at
//        Integer custID = 56; // Integer | customer ID making purchase
//        String date = "20210101";
//
//        try {
//            ApiResponse apiResponse = apiInstance.newPurchaseWithHttpInfo(body, storeID, custID, date);
//            System.out.println(apiResponse.getData());
//            System.out.println(apiResponse.getHeaders());
//            System.out.println(apiResponse.getStatusCode());
//        } catch (ApiException e) {
//            System.err.println("Exception when calling PurchaseApi#newPurchase");
//            e.printStackTrace();
//        }
    }


}
