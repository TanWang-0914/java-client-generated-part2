public class Phase {
    public String currentPhase = "EastPhaseStart";

    public synchronized void changePhase (String newPhase){
        synchronized (currentPhase){
            currentPhase = newPhase;
            System.out.println(currentPhase);
        }
    }

    public String getPhase (){
//        String res;
//        synchronized (currentPhase){
//            res = new String(currentPhase);
//        }
//        return res;
        return currentPhase;
    }
}
