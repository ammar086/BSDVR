package app;

import java.net.Socket;
import java.time.Instant;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

public class Neighbor{
    private Integer id;
    private Socket link;
    private Long a_timer;
    private Long c_timer;
    private Integer l_cost;
    private Integer l_state;
    private Thread l_manager;
    private ConcurrentHashMap<Integer,InputStream> c_read;
    private ConcurrentHashMap<Integer,OutputStream> c_write;

    public Neighbor(){
        id = -1;
        link = null;
        l_cost = -1;
        l_state = 0;
        l_manager = null;
        a_timer = (long) -1;
        c_timer = (long) -1;
        c_read = null;
        c_write = null;
    }

    public Neighbor(Integer i, Socket l, Integer l_c,Integer l_s, Thread l_m, 
                    Long a_t,Long c_t, InputStream c_r, OutputStream c_w){
        id = i;
        link = l;
        l_cost = l_c;
        l_state = l_s;
        l_manager = l_m;
        a_timer = a_t;
        c_timer = c_t;
        c_read = new ConcurrentHashMap<Integer,InputStream>();
        c_read.put(i,c_r);
        c_write = new ConcurrentHashMap<Integer,OutputStream>();
        c_write.put(i,c_w);
    }

    //getters
    public Integer getID(){return id;}
    public Socket getSocket(){return link;}
    public Integer getLinkCost(){return l_cost;}
    public Integer getLinkState(){return l_state;}
    public Thread getLinkManager(){return l_manager;}
    public Long getTimer(){return c_timer;}
    public InputStream getRead(Integer n){return c_read.get(n);}
    public OutputStream getWrite(Integer n){return c_write.get(n);}
    //setters
    public void setTimer(Long n_timer){c_timer = n_timer;}
    public void setLinkCost(Integer n_cost){l_cost = n_cost;}
    public void setLinkState(Integer n_state){l_state = n_state;}
    //methods
    @Override 
    public String toString(){
        long diff = Instant.now().toEpochMilli() - a_timer;
        long period = TimeUnit.MILLISECONDS.toSeconds(diff);
        return "{link-cost: "+Integer.toString(l_cost)+
               ", link-state: "+Integer.toString(l_state)+
               ", timeElapsed: "+Long.toString(period)+"s}";
    }

}