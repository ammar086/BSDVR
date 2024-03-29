package app;

import java.time.Instant;
import java.io.FileWriter;
import java.util.ArrayList;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Tstat { // class to collect network statistics
    // stats per event
    private ReentrantLock lockA;
    private ReentrantLock lockB;
    private ArrayList<Integer> ctally;
    private ArrayList<Integer> stally;
    private Integer reply_update_count;
    private Integer reply_update_bytes;
    private ConcurrentHashMap<Integer, ArrayList<TSMessage>> messages;
    // stats per experiment
    private ConcurrentHashMap<Integer, Integer> eructally;
    private ConcurrentHashMap<Integer, Integer> erustally;
    private ConcurrentHashMap<Integer, ArrayList<Integer>> ectally;
    private ConcurrentHashMap<Integer, ArrayList<Integer>> estally;
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ArrayList<TSMessage>>> emessages;

    public Tstat(Integer id) {
        lockA = new ReentrantLock();
        lockB = new ReentrantLock();
        ctally = new ArrayList<Integer>();
        stally = new ArrayList<Integer>();
        eructally = new ConcurrentHashMap<Integer, Integer>();
        erustally = new ConcurrentHashMap<Integer, Integer>();
        ectally = new ConcurrentHashMap<Integer, ArrayList<Integer>>();
        estally = new ConcurrentHashMap<Integer, ArrayList<Integer>>();
        messages = new ConcurrentHashMap<Integer, ArrayList<TSMessage>>();
        emessages = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ArrayList<TSMessage>>>();
        reply_update_count = 0;
        reply_update_bytes = 0;
        for (Integer i = 0; i < 5; i++) {
            ctally.add(0);
            stally.add(0);
            messages.put(i + 1, new ArrayList<TSMessage>());
        }
    }

    public ConcurrentHashMap<Integer, Integer> getReplyUpdateCTally(){
        return eructally;
    }

    public ConcurrentHashMap<Integer, Integer> getReplyUpdateSTally(){
        return erustally;
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
                        " " + estally.get(i) + " " + sumTally(estally.get(i)) +
                        " " + eructally.get(i) + " " + erustally.get(i)
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
        while(true){
            if(ctally.size() == 5){
                if(pid >= 0 && pid < 5 ){
                    ctally.set(pid, ctally.get(pid) + 1);
                }
                break;
            }
        }
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
        while(true){
            ArrayList<TSMessage> updated;
            if(messages.size() == 5){
                if(messages.containsKey(type)){
                    updated = messages.get(type);
                    updated.add(new TSMessage(m,tsa,tsb));
                    messages.replace(type,updated);
                    break;
                }
            }
        }
    }

    public void addEvent(Integer eid){
        // assume ctally has been computed
        if(eid >= 0){
            ectally.put(eid, ctally);
            estally.put(eid, stally);
            eructally.put(eid, reply_update_count);
            erustally.put(eid, reply_update_bytes);
            emessages.put(eid, messages);
        }
        ctally = new ArrayList<Integer>();
        stally = new ArrayList<Integer>();
        reply_update_count = 0;
        reply_update_bytes = 0;
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

    public synchronized void pendingReplyUpdate(Message m){
        reply_update_count += 1;
        reply_update_bytes += m.getSize();
    }

    public synchronized void sendMessageUpate(Integer type, Message m){
        Long tmp;
        countPkt(type-1);
        countBytes(type-1, m);
        tmp = Instant.now().toEpochMilli();
        countMessages(1, m,tmp,tmp);
    }

    public synchronized void receiveMessageUpdate(Integer type, Message m, ArrayList<Integer> changes, Long tmp){
        countPkt(type-1);
        countBytes(type-1,m);
        if(changes.isEmpty()){
            // no changes in FT timestamp
            countMessages(type,m,tmp,Instant.now().toEpochMilli());
        }else{
            // changes in FT timestamp
            tmp = Instant.now().toEpochMilli();
            countMessages(type,m,tmp,tmp);
        }
    }
}