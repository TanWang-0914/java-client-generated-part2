public class ReqCount {

    public int[][] successfulReq;
    public int[][] failedReq;

    public ReqCount(int maxStoreID, int opHours){
        successfulReq = new int[maxStoreID][opHours];
        failedReq = new int[maxStoreID][opHours];
    }

    public synchronized void incSuc (int storeID, int hour){
        successfulReq[storeID][hour]++;
    }

    public synchronized void incFail (int storeID, int hour){
        failedReq[storeID][hour]++;
    }
}
