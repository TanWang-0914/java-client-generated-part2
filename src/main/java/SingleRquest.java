import io.swagger.client.*;
import io.swagger.client.model.*;
import io.swagger.client.api.PurchaseApi;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class SingleRquest implements Runnable{
    private int storeID, custID, numberOfItem, maxItemID;
    private String purchaseDate;
    PurchaseApi apiInstance;
    Logger logger = org.apache.log4j.Logger.getLogger(SingleRquest.class);

    Random rand = new Random();
    ReqCount reqCount;
    int hour;
    BlockingQueue<String[]> queue;

    public SingleRquest(int hour,int store, int cust, String date, int maxItem, int numItem, PurchaseApi apiInstance, ReqCount reqCount,BlockingQueue<String[]> queue){
        this.hour = hour;
        storeID = store;
        custID = cust;
        numberOfItem = numItem;
        maxItemID = maxItem;
        purchaseDate = date;
        this.apiInstance = apiInstance;
        this.reqCount = reqCount;
        this.queue = queue;
    }

    @Override
    public void run() {
        Purchase body = new Purchase();
        for (int i = 0; i < numberOfItem; i++){
            int currentItemId = rand.nextInt(maxItemID)+1;
            PurchaseItems currentItem = new PurchaseItems();
            currentItem.setItemID(String.valueOf(currentItemId));
            currentItem.setNumberOfItems(1);
            body.addItemsItem(currentItem);
        }

        try {
            long reqStartTime = System.currentTimeMillis();
            ApiResponse apiResponse = apiInstance.newPurchaseWithHttpInfo(body, storeID, custID, purchaseDate);
//            System.out.println(apiResponse.getData());
//            System.out.println(apiResponse.getHeaders());
            int stausCode = apiResponse.getStatusCode();
            if (stausCode == 200 || stausCode == 201){
                reqCount.incSuc(storeID,hour);
            }else {
                logger.info("StatusCode-" + stausCode + ": " + "/purchase/"+storeID+"/customer/"+custID+"/date/"+purchaseDate);
                reqCount.incFail(storeID,hour);
            }
            long reqEndTime = System.currentTimeMillis();
            String[] tempData = {String.valueOf(reqStartTime),"POST", String.valueOf(reqEndTime-reqStartTime),String.valueOf(stausCode)};
            queue.put(tempData);
        } catch (ApiException | InterruptedException e) {
            System.err.println("Exception when calling PurchaseApi#newPurchase");
            e.printStackTrace();
        }
    }
}
