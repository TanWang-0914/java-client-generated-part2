public class ReqCount {

    public int failedReq;

    public ReqCount(int maxStoreID, int opHours){
        failedReq = 0;
    }

    public synchronized void incFail (){
        failedReq++;
    }
}
