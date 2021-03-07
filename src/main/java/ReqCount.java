public class ReqCount {

    public int succReq;

    public ReqCount(int maxStoreID, int opHours){
        succReq = 0;
    }

    public synchronized void incSuc (int num){
        succReq+= num;
    }
}
