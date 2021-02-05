import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.PurchaseApi;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;

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
    BlockingQueue<String[]> queue;

    public RequestsPerStore(int storeID, int maxCustID, int maxItemID, int numPurchases, int numItemPerPurchase,String date, int opHour,PurchaseApi apiInstance, Phase phase,ReqCount reqCount, BlockingQueue<String[]> queue){
        this.storeID = storeID;
        this.maxCustID = maxCustID;
        this.maxItemID = maxItemID;
        this.numPurchases = numPurchases;
        this.numItemPerPurchase = numItemPerPurchase;
        this.date = date;
        this.opHour = opHour;
        this.apiInstance = apiInstance;
        this.phase = phase;
        successfulReq = new int[12];
        failedReq = new int[12];
        this.reqCount = reqCount;
        this.queue = queue;
    }

    @Override
    public void run() {
        for (int hour = 0; hour < opHour; hour++){
            for (int i = 0; i < numPurchases; i++){
                int currentCustID = rand.nextInt(maxCustID)+1000*storeID;

                SingleRquest singleReq = new SingleRquest(hour,storeID,currentCustID,date,maxItemID,numItemPerPurchase,apiInstance,reqCount,queue);
                Thread currentReq = new Thread(singleReq);
                currentReq.start();
                try {
                    currentReq.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            purchasesCount += numPurchases;
            if (purchasesCount == 3 * numPurchases && phase.getPhase().equals("EastPhaseRunning"))
                phase.changePhase("CentralPhaseStart");
            if (purchasesCount == 5 * numPurchases && phase.getPhase().equals("CentralPhaseRunning"))
                phase.changePhase("WestPhaseStart");
        }
    }
}
