package app;

import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class Protocols {
    public Protocols(){}

    public Message createBroadcastUpdate(ByteBuffer udvt, Router r, Integer neighbor, Integer type, Integer num_updates, ArrayList<Integer> changes){
        Message m;
        Boolean flag;
        Integer cost, state, curr_next_hop;
        flag = false;
        m = new SYN(1,0,0);
        for(Integer dest:changes){
            try {
                if(dest != neighbor){
                    if(dest >= Constants.ID_MIN && dest <= Constants.ID_MAX){
                        if(r.getFT().containsKey(dest)){
                            curr_next_hop = r.getCurrentNextHopFT(dest);
                            if(r.getFT().get(dest).containsKey(curr_next_hop)){
                                state = r.getFT().get(dest).get(curr_next_hop).getState();
                                // poison reverse
                                cost = (curr_next_hop == neighbor) ? Integer.MAX_VALUE: r.getFT().get(dest).get(curr_next_hop).getCost();
                                udvt.putInt(dest);
                                udvt.putInt(cost);
                                udvt.putInt(state);
                                flag = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                r.printException(e);
                if(type == 0){
                    System.out.println("\nentry not available at "+Router.translateID(r.getID())+" for : "+Router.translateID(dest)+" in broadcast-a");
                }else if (type == 2){
                    if(!e.toString().contains("NullPointerException")){ // if ft value not updated
                        System.out.println(changes.toString());
                        System.out.println("\nentry not available at "+Router.translateID(r.getID())+" for : "+Router.translateID(dest)+" in broadcast-b");
                    }
                }
            }
        }
        if(flag)m = new UPDATE(5,r.getID(),neighbor,num_updates,udvt.array());
        return m;
    }

    public synchronized void broadcast(Router r, ArrayList<Integer> ch, Integer sender, Integer type) {
        for(Integer l: r.getLT().keySet()){       
            Message m;
            ByteBuffer udvt;
            // Boolean f = false;
            Integer n;//, cvia, cost, state;
            Socket sock = r.getLT().get(l).getSocket();
            OutputStream out = r.getLT().get(l).getWrite(l);
            if(r.getLT().get(l).getLinkState() == 1){
                if((sender == l && type == 1) || (sender == l && type == 2)){    // send complete dv to new neighbors
                    n = r.getFT().size() - 1;
                    udvt = ByteBuffer.allocate(n*12);
                    ch = new ArrayList<Integer>(r.getFT().keySet());
                    m = createBroadcastUpdate(udvt,r,l,0,n,ch);
                }else{    // send only changes to old neighbors
                    n = (ch.contains(l)) ? (ch.size() - 1) : ch.size();
                    udvt = ByteBuffer.allocate(n*12);
                    m = createBroadcastUpdate(udvt,r,l,1,n,ch);
                }
                if(m.getType() == 5){
                    try {
                        if(!sock.isClosed()){r.send(out, m);}
                    } catch (Exception e) {
                        System.out.println("Broadcast error at "+Router.translateID(r.getID())+" for neighbor: "+Router.translateID(l));
                    }
                }
            }
        }
    }

    public synchronized void retransmit(UPDATE u, Router r){
        Message m;
        OutputStream out;
        long timer, timeElapsed, timeDiff;
        ByteBuffer vec_in_update, vec_available;
        ArrayList<Integer> old_vec_rt, final_vec_rt;
        Integer neighbor, num_updates, next_hop, curr_dest, curr_cost_c2, curr_state;
        Integer next_hop_cost_l2, curr_dest_cost_c1, next_hop_curr_dest_cost_c3, next_hop_neighbor_cost_c5, curr_dest_state;
        neighbor = u.getSender();
        out = r.getLT().get(neighbor).getWrite(neighbor);
        try {
            // respond only if link with neighbor still active
            if(r.getLT().get(neighbor).getLinkState() == 1){
                num_updates = u.getNUpdates();
                old_vec_rt = new ArrayList<Integer>();
                final_vec_rt = new ArrayList<Integer>();
                vec_in_update = ByteBuffer.wrap(u.getUpdates());
                for(int i = 0; i < num_updates; i++){
                    curr_dest = vec_in_update.getInt();
                    // c2
                    curr_cost_c2 = vec_in_update.getInt();
                    curr_state = vec_in_update.getInt();
                    // respond  only for inactive updates
                    if(curr_state == 0 && curr_cost_c2 !=  Integer.MAX_VALUE){
                        // check if active entry available in FT
                        if(r.getFT().containsKey(curr_dest)){
                            next_hop = r.getCurrentNextHopFT(curr_dest);
                            if(r.getFT().get(curr_dest).get(next_hop).getState() == 1){
                                if(next_hop != neighbor && curr_dest != neighbor){
                                    // l2
                                    next_hop_cost_l2 = r.getLT().get(next_hop).getLinkCost();
                                    // c1
                                    curr_dest_cost_c1 = r.getFT().get(curr_dest).get(next_hop).getCost();
                                    // c3
                                    next_hop_curr_dest_cost_c3 = curr_dest_cost_c1 - next_hop_cost_l2;
                                    // c5
                                    next_hop_neighbor_cost_c5 = r.getDVT().get(next_hop).get(neighbor).getCost();
                                    if(next_hop_curr_dest_cost_c3 == 0 || next_hop_neighbor_cost_c5 == curr_cost_c2 + next_hop_curr_dest_cost_c3){
                                        // send immediate reply
                                        curr_dest_state = r.getFT().get(curr_dest).get(next_hop).getState();
                                        m = new UPDATE(5,r.getID(),neighbor,1,curr_dest,curr_dest_cost_c1,curr_dest_state);
                                        if(!r.getLT().get(neighbor).getSocket().isClosed()){r.send(out,m);}
                                    }else{
                                        // start queue for pending reply timer
                                        old_vec_rt.add(curr_dest);
                                    }
                                }
                            }
                        }
                    }
                }
                if(old_vec_rt.size() > 0){
                    // invoke peinding reply timer
                    timeElapsed = 0;
                    timer = Instant.now().toEpochMilli();
                    while(timeElapsed < Constants.TIMER_BASE){
                        timeDiff = Instant.now().toEpochMilli() - timer;
                        timeElapsed = TimeUnit.MILLISECONDS.toMillis(timeDiff);
                    }
                    // seperate active entries avaialble
                    for(Integer dest:old_vec_rt){
                        if(r.getFT().containsKey(dest)){
                            next_hop = r.getCurrentNextHopFT(dest);
                            curr_dest_state = r.getFT().get(dest).get(next_hop).getState();
                            if(curr_dest_state == 1){
                                final_vec_rt.add(dest);
                            }
                        }
                    }
                    // send reply for pending active entries available
                    vec_available = ByteBuffer.allocate(final_vec_rt.size() * 12);
                    for(Integer dest:final_vec_rt){
                        next_hop = r.getCurrentNextHopFT(dest);
                        curr_dest_cost_c1 = r.getFT().get(dest).get(next_hop).getCost();
                        curr_dest_state = r.getFT().get(dest).get(next_hop).getState();
                        vec_available.putInt(dest);
                        vec_available.putInt(curr_dest_cost_c1);
                        vec_available.putInt(curr_dest_state);
                    }
                    m = new UPDATE(5,r.getID(),neighbor,final_vec_rt.size(),vec_available.array());
                    if(!r.getLT().get(neighbor).getSocket().isClosed()){r.send(out,m);}
                }
            }
        } catch (Exception e) {
            r.printExceptionWithNeighbor(e,Router.translateID(r.getID()),Router.translateID(neighbor));
        }
    }

    public void BSDVRP(Router r, Message m, InputStream in, OutputStream out, Socket link){
        Long tmp;
        Integer tmp2;
        Message reply;
        Vector new_vec;
        ArrayList<Integer> ch;
        DataPacketHandler dp = null;
        switch (m.getType()) {
            case 1: // SYN
                SYN s = (SYN)m;
                tmp = Instant.now().toEpochMilli();
                // add to link table
                r.add_LT(s.getSender(),link,s.getCost(),1,in,out);
                // reply with SYN-ack
                reply = new SYN_ACK(2,r.getID(),s.getSender(),s.getCost());
                r.send(out,reply);
                // update entries in dvt
                new_vec = new Vector(s.getCost(),1);
                r.updateDVT(s.getSender(),s.getSender(),new_vec);
                // re-compute ft
                ch = r.computeFT();
                // measurement for convergence time and control traffic
                r.getCstats().receiveMessageUpdate(1,m,ch,tmp);
                // broadcast changes
                broadcast(r,ch,s.getSender(),1);
                // call data packet handler - on meeting new neighbor
                dp = new DataPacketHandler(r, s.getSender());
                break;
            case 2: // SYN_ACK
                SYN_ACK sa = (SYN_ACK)m;
                tmp = Instant.now().toEpochMilli();
                // add to link table
                r.add_LT(sa.getSender(),link,sa.getCost(),1,in,out);
                // update entries in dvt
                new_vec = new Vector(sa.getCost(),1);
                r.updateDVT(sa.getSender(),sa.getSender(),new_vec);
                // re-compute ft
                ch = r.computeFT();
                // measurement for convergence time and control traffic
                r.getCstats().receiveMessageUpdate(2,m,ch,tmp);
                // broadcast changes
                broadcast(r,ch,sa.getSender(),2);
                // call data packet handler  - on meeting new neighbor
                dp = new DataPacketHandler(r, sa.getSender());
                break;
            case 3: // FIN
                FIN f = (FIN) m;
                // reply with FIN_ACK
                reply = new FIN_ACK(4,r.getID(),f.getSender());
                r.send(out,reply);
                r.setCMessage(m);
                r.getLT().get(f.getSender()).setLinkState(0);
                break;
            case 4: // FIN_ACK
                FIN_ACK fa = (FIN_ACK) m;
                r.setCMessage(m);
                r.getLT().get(fa.getSender()).setLinkState(0);
                break;
            case 5: // UPDATE
                UPDATE u = (UPDATE)m;
                tmp2 = r.getUCount();
                r.setUCount(tmp2 + 1);
                tmp = Instant.now().toEpochMilli();
                Integer dest, cost, state, num_updates, stateCount;
                ByteBuffer updates = ByteBuffer.wrap(u.getUpdates());
                // update entries in dvt
                stateCount = 0;
                num_updates = u.getNUpdates();
                for (int i = 0; i < num_updates; i++) {
                    dest = updates.getInt();
                    if(dest >= Constants.ID_MIN && dest <= Constants.ID_MAX){
                        cost = updates.getInt();
                        state = updates.getInt();
                        new_vec = new Vector(cost,state);
                        r.updateDVT(u.getSender(),dest,new_vec);
                        if(!cost.equals(Integer.MAX_VALUE) && state.equals(0)){stateCount = stateCount + 1;}
                    }
                }
                // re-compute ft
                ch = r.computeFT();
                // measurement for convergence time and control traffic
                r.getCstats().receiveMessageUpdate(5,m,ch,tmp);
                // broadcast changes
                broadcast(r, ch, u.getSender(), 5);
                // call data packet handler - on update in ft
                if(!ch.isEmpty()){dp = new DataPacketHandler(r, ch);}
                //propagate new primary path upstream
                if(stateCount > 0){retransmit(u,r);}
                break;
            case 6: // HELLO
                HELLO h = (HELLO)m;
                // update link timer
                r.getLT().get(h.getSender()).setTimer(Instant.now().toEpochMilli());
                break;
            case 7:
                // call data packet handler - on receiving data summary message
                dp = new DataPacketHandler(r, false, m);
                break;
            case 8:
                // call data packet handler - on receiving data payload message
                tmp2 = r.getDCount();
                r.setDCount(tmp2+1);
                dp = new DataPacketHandler(r, true, m);
                break;
        }
        if(dp!=null){dp=null;}
    }   
}
