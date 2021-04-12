package app;

import java.time.Instant;
import java.util.Iterator;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PendingReplyQueue implements Runnable{
    static class Entry{
        private Long timestamp;
        private Integer neighbor;
        private ArrayList<Integer> destinations;
        //constructor
        public Entry(Long ts, Integer n, ArrayList<Integer> dests){
            timestamp = ts;
            neighbor = n;
            destinations = new ArrayList<Integer>();
            for(Integer d:dests)destinations.add(d);
        }
        //getters
        public Long getTimeStamp(){return timestamp;}
        public Integer getNeighbor(){return neighbor;}
        public ArrayList<Integer> getDestinations(){return destinations;}
    }
    //elements
    private Router router;
    private Integer curr_neighbor;
    private ArrayList<Integer> curr_destinations;
    private ConcurrentLinkedQueue<Entry> reply_queue;
    //constructor    
    public PendingReplyQueue(Router r){
        router = r;
        reply_queue = new ConcurrentLinkedQueue<Entry>();
    }
    //methods
    public synchronized void removeReply(){
        Entry tmp;
        Iterator<Entry> iter;
        Long curr_time, timeDiff, timeElapsed; 
        // initializing values
        curr_neighbor = -1;
        iter = reply_queue.iterator();
        curr_time = Instant.now().toEpochMilli();
        curr_destinations = new ArrayList<Integer>();
        while(iter.hasNext()){
            tmp = iter.next();
            timeDiff = curr_time - tmp.getTimeStamp();
            timeElapsed = TimeUnit.MILLISECONDS.toMillis(timeDiff);
            if(timeElapsed > Constants.TIMER_BASE){
                curr_neighbor = tmp.getNeighbor();
                curr_destinations = new ArrayList<Integer>();
                for(Integer dest:tmp.getDestinations()){curr_destinations.add(dest);}
                reply_queue.remove(tmp);
                iter = reply_queue.iterator();
                break;
            }
        }
    }
    public synchronized void insertReply(Long timestamp, Integer neighbor, ArrayList<Integer> destinations){
        if(destinations.size() > 0){
            Entry new_entry;
            new_entry = new Entry(timestamp, neighbor, destinations);
            reply_queue.add(new_entry);
        }
    }
    @Override
    public void run() {
        Message m;
        OutputStream out;
        ByteBuffer vec_available;
        ArrayList<Integer> final_vec_rt;
        Integer next_hop, curr_dest_state, curr_dest_cost_c1;
        while(router.getRFlag()){
            try {
                if(reply_queue.size() > 0){
                    removeReply();
                    final_vec_rt = new ArrayList<Integer>();
                    if(curr_destinations.size() > 0 && router.getLT().get(curr_neighbor).getLinkState() == 1){
                        out = router.getLT().get(curr_neighbor).getWrite(curr_neighbor);
                        // seperate active entries avaialble
                        for(Integer dest:curr_destinations){
                            if(router.getFT().containsKey(dest)){
                                next_hop = router.getCurrentNextHopFT(dest);
                                curr_dest_state = router.getFT().get(dest).get(next_hop).getState();
                                if(curr_dest_state == 1){final_vec_rt.add(dest);}
                            }
                        }
                        // send reply for pending active entries available
                        vec_available = ByteBuffer.allocate(final_vec_rt.size() * 12);
                        for(Integer dest:final_vec_rt){
                            next_hop = router.getCurrentNextHopFT(dest);
                            curr_dest_cost_c1 = router.getFT().get(dest).get(next_hop).getCost();
                            curr_dest_state = router.getFT().get(dest).get(next_hop).getState();
                            vec_available.putInt(dest);
                            vec_available.putInt(curr_dest_cost_c1);
                            vec_available.putInt(curr_dest_state);
                        }
                        m = new UPDATE(5,router.getID(),curr_neighbor,final_vec_rt.size(),vec_available.array());
                        if(!router.getLT().get(curr_neighbor).getSocket().isClosed()){
                            router.getDCstats().pendingReplyUpdate(m);
                            router.send(out,m);
                        }
                    }
                }
                TimeUnit.MILLISECONDS.sleep(Constants.TIMER_BASE / 100);
            } catch (Exception e) {}
        }
    }   
}

