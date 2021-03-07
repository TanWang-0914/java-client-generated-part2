import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.PurchaseApi;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class RequestsPerStore implements Runnable{
    private int storeID, maxCustID, maxItemID, numPurchases, numItemPerPurchase;
    private String date;
    Random rand = new Random();
    public int purchasesCount = 0;
    PurchaseApi apiInstance;
    Phase phase;
    ReqCount reqCount;
    int[] successfulReq;
    int[] failedReq;
    int opHour;
    CountDownLatch centralPhaseLatch;
    CountDownLatch westPhaseLatch;
    Logger logger = LoggerFactory.getLogger(RequestsPerStore.class);
    BlockingQueue<String[]> queue;

    public RequestsPerStore(int storeID, int maxCustID, int maxItemID, int numPurchases, int numItemPerPurchase, String date, int opHour, String basePath, CountDownLatch centralPhaseLatch, CountDownLatch westPhaseLatch, ReqCount reqCount, BlockingQueue<String[]> queue){
        PurchaseApi apiInstance = new PurchaseApi();
        apiInstance.getApiClient().setBasePath(basePath);
        apiInstance.getApiClient().setConnectTimeout(100000);
        apiInstance.getApiClient().setReadTimeout(100000);
        apiInstance.getApiClient().setWriteTimeout(100000);

        this.storeID = storeID;
        this.maxCustID = maxCustID;
        this.maxItemID = maxItemID;
        this.numPurchases = numPurchases;
        this.numItemPerPurchase = numItemPerPurchase;
        this.date = date;
        this.opHour = opHour;
        this.apiInstance = apiInstance;
        successfulReq = new int[12];
        failedReq = new int[12];
        this.reqCount = reqCount;
        this.centralPhaseLatch = centralPhaseLatch;
        this.westPhaseLatch = westPhaseLatch;
        this.queue = queue;
    }

    @Override
    public void run() {
        int succCount = 0;
        for (int hour = 0; hour < opHour; hour++){
            for (int i = 0; i < numPurchases; i++){

                // generate customer ID
                int currentCustID = rand.nextInt(maxCustID) + 1000 * storeID;

                // generate purchase body, items + number of each item
                Purchase body = new Purchase();
                for (int j = 0; j < numItemPerPurchase; j++) {
                    int currentItemId = rand.nextInt(maxItemID) + 1;
                    PurchaseItems currentItem = new PurchaseItems();
                    currentItem.setItemID(String.valueOf(currentItemId));
                    currentItem.setNumberOfItems(1);
                    body.addItemsItem(currentItem);
                }

                try {
                     long reqStartTime = System.currentTimeMillis();

                    // retry mechanism,  201 response code : write to database success, 200 response code : write to database failed
                    // if got 200 code, retry post request up to 3 times
                    // if got 201 code, count success req, break for loop
                    int statusCode = 200;
                    for (int k = 0; k < 3; k++) {
                        ApiResponse apiResponse = apiInstance.newPurchaseWithHttpInfo(body, storeID, currentCustID, date);

                        statusCode = apiResponse.getStatusCode();

                        if (statusCode == 201) {
                            succCount++;
                            break;
                        } else {
                            // logger.error("StatusCode-" + statusCode + ": " + "/purchase/" + storeID + "/customer/" + currentCustID + "/date/" + date);
                        }
                    }

                    long reqEndTime = System.currentTimeMillis();
                    String[] tempData = {String.valueOf(reqStartTime),"POST", String.valueOf(reqEndTime-reqStartTime),String.valueOf(statusCode)};
                    queue.put(tempData);
                }catch (ApiException | InterruptedException e) {
                    System.err.println("Exception when calling PurchaseApi#newPurchase");
                    e.printStackTrace();
                }
            }
            // if finished first 3 hours' purchase, start centralPhase
            // if finished first 5 hours' purchase, start centralPhase
            purchasesCount += numPurchases;
            if (purchasesCount == 3 * numPurchases)
                centralPhaseLatch.countDown();
            if (purchasesCount == 5 * numPurchases)
                westPhaseLatch.countDown();
        }
        // add success purchases number to count
        reqCount.incSuc(succCount);
    }
}
