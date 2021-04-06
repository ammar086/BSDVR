package app;

import java.time.Instant;
import java.util.Iterator;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PendingReplyQueue implements Runnable{
    //elements
    private Router router;
    private Integer curr_neighbor;
    private ArrayList<Integer> curr_destinations;
    private ConcurrentLinkedQueue<Long> reply_timers;
    private ConcurrentHashMap<Long, Integer> reply_neighbors;
    private ConcurrentHashMap<Long,ArrayList<Integer>> reply_destinations;
    //constructor    
    public PendingReplyQueue(Router r){
        router = r;
        reply_timers = new ConcurrentLinkedQueue<Long>();
        reply_neighbors = new ConcurrentHashMap<Long, Integer>();
        reply_destinations = new ConcurrentHashMap<Long, ArrayList<Integer>>();
    }
    //methods
    public synchronized void removeReply(){
        // System.out.println("removeReply()");
        Iterator<Long> iter;
        Long tmp, curr_time, timeDiff, timeElapsed; 
        // initializing values
        curr_neighbor = -1;
        iter = reply_timers.iterator();
        curr_time = Instant.now().toEpochMilli();
        curr_destinations = new ArrayList<Integer>();
        while(iter.hasNext()){
            tmp = iter.next();
            timeDiff = curr_time - tmp;
            timeElapsed = TimeUnit.MILLISECONDS.toMillis(timeDiff);
            if(timeElapsed > Constants.TIMER_BASE){
                curr_neighbor = reply_neighbors.get(tmp);
                curr_destinations = new ArrayList<Integer>();
                for(Integer dest:reply_destinations.get(tmp)){curr_destinations.add(dest);}
                // System.out.println("Removing");
                // System.out.print(Router.translateID(curr_neighbor) + " " + reply_timers.size() + " " + reply_neighbors.size() + " " + reply_destinations.size() + " [");
                // for(Integer d:curr_destinations)System.out.print(" " + d);
                // System.out.println(" ]");
                // System.out.println(reply_timers.size() + " " + reply_neighbors.size() + " " + reply_destinations.size());
                reply_timers.remove(tmp);
                reply_neighbors.remove(tmp);
                reply_destinations.remove(tmp);
                iter = reply_timers.iterator();
                break;
            }
        }
    }
    public synchronized void insertReply(Long timestamp, Integer neighbor, ArrayList<Integer> destinations){
        if(destinations.size() > 0){
            // System.out.println("\ninsertReply()");
            reply_timers.add(timestamp);
            reply_neighbors.put(timestamp, neighbor);
            reply_destinations.put(timestamp, destinations);
            // System.out.print(Router.translateID(neighbor) + " " + reply_timers.size() + " " + reply_neighbors.size() + " " + reply_destinations.size() + " [");
            // for(Integer d:destinations)System.out.print(" " + d);
            // System.out.println(" ]");
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
            // System.out.println("HERE");
            try {
                if(reply_destinations.size() > 0  && reply_timers.size() > 0 && reply_neighbors.size() > 0){
                    removeReply();
                    final_vec_rt = new ArrayList<Integer>();
                    if(curr_destinations.size() > 0 && router.getLT().get(curr_neighbor).getLinkState() == 1){
                        out = router.getLT().get(curr_neighbor).getWrite(curr_neighbor);
                        // System.out.print("[ ");
                        // for(Integer d:curr_destinations)System.out.print(" " + d);
                        // System.out.println(" ]");
                        // seperate active entries avaialble
                        for(Integer dest:curr_destinations){
                            if(router.getFT().containsKey(dest)){
                                next_hop = router.getCurrentNextHopFT(dest);
                                curr_dest_state = router.getFT().get(dest).get(next_hop).getState();
                                if(curr_dest_state == 1){final_vec_rt.add(dest);}
                            }
                        }
                        // System.out.println("Available");
                        // System.out.print(Router.translateID(curr_neighbor) + " " + reply_timers.size() + " " + reply_neighbors.size() + " " + reply_destinations.size() + " [");
                        // for(Integer d:final_vec_rt)System.out.print(" " + d);
                        // System.out.println(" ]");
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
            } catch (Exception e) {}
        }
        // System.out.println(router.getID()+ ": HERE-BAD");
    }   
}

