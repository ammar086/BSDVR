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
        this.id = -1;
        this.link = null;
        this.l_cost = -1;
        this.l_state = 0;
        this.l_manager = null;
        this.a_timer = (long) -1;
        this.c_timer = (long) -1;
        this.c_read = null;
        this.c_write = null;
    }

    public Neighbor(Integer i, Socket l, Integer l_c,Integer l_s, Thread l_m, 
                    Long a_t,Long c_t, InputStream c_r, OutputStream c_w){
        this.id = i;
        this.link = l;
        this.l_cost = l_c;
        this.l_state = l_s;
        this.l_manager = l_m;
        this.a_timer = a_t;
        this.c_timer = c_t;
        this.c_read = new ConcurrentHashMap<Integer,InputStream>();
        this.c_read.put(i,c_r);
        this.c_write = new ConcurrentHashMap<Integer,OutputStream>();
        this.c_write.put(i,c_w);
    }

    //getters
    public Integer getID(){return this.id;}
    public Socket getSocket(){return this.link;}
    public Integer getLinkCost(){return this.l_cost;}
    public Integer getLinkState(){return this.l_state;}
    public Thread getLinkManager(){return this.l_manager;}
    public Long getTimer(){return this.c_timer;}
    public InputStream getRead(Integer n){return this.c_read.get(n);}
    public OutputStream getWrite(Integer n){return this.c_write.get(n);}
    //setters
    public void setTimer(Long n_timer){this.c_timer = n_timer;}
    public void setLinkCost(Integer n_cost){this.l_cost = n_cost;}
    public void setLinkState(Integer n_state){this.l_state = n_state;}
    //methods
    @Override 
    public String toString(){
        long diff = Instant.now().toEpochMilli() - this.a_timer;
        long period = TimeUnit.MILLISECONDS.toSeconds(diff);
        return "{link-cost: "+Integer.toString(this.l_cost)+
               ", link-state: "+Integer.toString(this.l_state)+
               ", timeElapsed: "+Long.toString(period)+"s}";
    }

}