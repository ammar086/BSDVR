package app;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Tstat { // class to collect network statistics
    // stats per event
    private ReentrantLock lockA;
    private ReentrantLock lockB;
    private ArrayList<Integer> ctally;
    private ArrayList<Integer> stally;
    private ConcurrentHashMap<Integer, ArrayList<TSMessage>> messages;
    // stats per experiment
    private ConcurrentHashMap<Integer, ArrayList<Integer>> ectally;
    private ConcurrentHashMap<Integer, ArrayList<Integer>> estally;
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ArrayList<TSMessage>>> emessages;

    public Tstat(Integer id) {
        lockA = new ReentrantLock();
        lockB = new ReentrantLock();
        ctally = new ArrayList<Integer>();
        stally = new ArrayList<Integer>();
        ectally = new ConcurrentHashMap<Integer, ArrayList<Integer>>();
        estally = new ConcurrentHashMap<Integer, ArrayList<Integer>>();
        messages = new ConcurrentHashMap<Integer, ArrayList<TSMessage>>();
        emessages = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ArrayList<TSMessage>>>();
        for (Integer i = 0; i < 5; i++) {
            ctally.add(0);
            stally.add(0);
            messages.put(i + 1, new ArrayList<TSMessage>());
        }
    }

    public ConcurrentHashMap<Integer, ArrayList<Integer>> getCTally() {
        return ectally;
    }

    public ConcurrentHashMap<Integer, ArrayList<Integer>> getSTally() {
        return estally;
    }

    public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ArrayList<TSMessage>>> getMessages() {
        return emessages;
    }

    public void printMessages(Integer eid) {
        if (emessages.size() > 0) {
            for (Integer i : emessages.keySet()) {
                if (i == eid) {
                    for (Integer t : emessages.get(i).keySet()) {
                        for (TSMessage ts : emessages.get(i).get(t)) {
                            System.out.println(i + " " + ts.printTSMessage());// ts.getMessage().toString());
                        }
                    }
                }
            }
        } else {
            lockA.lock();
            Integer count = 0;
            for (Integer i : messages.keySet()) {
                if (i == 5) {
                    count += 1;
                }
                for (TSMessage ts : messages.get(i)) {
                    System.out.println(i + " " + ts.printTSMessage());// ts.getMessage().toString());
                    if (count == 500) {
                        break;
                    }
                }
            }
            System.out.println("No event has occurred in the network !");
            lockA.unlock();
        }
    }

    public void printMessages(Integer eid, String type, Integer id) throws IOException {
        String path = System.getProperty("user.dir")+"/../src/app/Test/topology/" + Router.translateID(id) + ".txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(path,true));
        String res = "***"+Router.translateID(id)+":"+type+"***\n";
        writer.write(res);
        if(emessages.size() > 0){
            for(Integer i:emessages.keySet()){
                if(i == eid){
                    for(Integer t:emessages.get(i).keySet()){
                        for(TSMessage ts:emessages.get(i).get(t)){
                            System.out.println(i + " " + ts.printTSMessage());
                        }
                    }
                }
            }
        }else{
            lockA.lock();
            for(Integer i:messages.keySet()){
                for(TSMessage ts:messages.get(i)){
                    res = i + " " + ts.printTSMessage() + "\n";
                    writer.write(res); 
                }
            }
            lockA.unlock();
        }
        writer.flush();
        writer.close();
    }

    public void printStats(Integer eid){
        if(ectally.size() > 0){
            for(Integer i:ectally.keySet()){
                if(i == eid){
                    System.out.println(
                        i + " " + ectally.get(i) + " " + sumTally(ectally.get(i)) +
                        " " + estally.get(i) + " " + sumTally(estally.get(i))
                    );
                }
            }
        }else{
            lockB.lock();
            Integer inx = 0;
            for(Integer i:ctally){
                System.out.print(inx + ":" + i+" ");
                inx += 1;
            }
            System.out.println("\nNo event has occurred in the network !");
            lockB.unlock();
        }
    }

    public void countPkt(Integer pid){
        lockB.lock();
        while(true){
            if(ctally.size() == 5){
                if(pid >= 0 && pid < 5 ){
                    ctally.set(pid, ctally.get(pid) + 1);
                }
                break;
            }
        }
        lockB.unlock();
    }

    public void countBytes(Integer pid, Message m){
        while(true){
            if(stally.size() == 5){
                if(pid >= 0 && pid < 5 ){
                    stally.set(pid, stally.get(pid) + m.getSize());
                }
                break;
            }
        }
    }

    public void countMessages(Integer type, Message m, long tsa, long tsb){
        lockA.lock();
        while(true){
            ArrayList<TSMessage> updated;
            if(messages.size() == 5){
                if(messages.containsKey(type)){
                    synchronized(this){
                        updated = messages.get(type);
                        updated.add(new TSMessage(m,tsa,tsb));
                        messages.replace(type,updated);
                        break;
                    }
                }
            }
        }
        lockA.unlock();
    }

    public void addEvent(Integer eid){
        // assume ctally has been computed
        if(eid >= 0){
            ectally.put(eid, ctally);
            estally.put(eid, stally);
            emessages.put(eid, messages);
        }
        ctally = new ArrayList<Integer>();
        stally = new ArrayList<Integer>();
        messages = new ConcurrentHashMap<Integer, ArrayList<TSMessage>>(); 
        for(Integer i = 0; i < 5; i ++){
            ctally.add(0);
            stally.add(0);
            messages.put(i+1, new ArrayList<TSMessage>());
        }
    }

    public static Integer sumTally(ArrayList<Integer> tally){
        Integer sum = 0;
        for(Integer i:tally){
            sum = sum + i;
        }
        return sum;
    }
}