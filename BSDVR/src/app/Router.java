package app;

import java.io.File;
import java.net.Socket;
import java.util.Arrays;
import java.time.Instant;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.io.BufferedInputStream;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Router {
    public static Integer id_min = 11;
    public static Integer id_max = 210;
    public static Integer port_min = 1234;
    public static Integer port_max = 1434;

    public static Integer MTU = 5000;
    public static Integer timerBase = 100;
    public static Integer DIGEST_SIZE = 20;
    public static Integer capacity = Integer.MAX_VALUE;
    public static String DEFAULT_DIGEST_ALGO = "SHA-1";
    public String path = System.getProperty("user.dir");
    public static Integer threshold = Integer.MAX_VALUE;

    // Control Plane
    private Integer id;
    private Boolean dflag;
    private Boolean hflag;
    private Integer ucount;
    private Integer dcount;
    private ServerSocket server;
    private ConcurrentHashMap<Integer, Neighbor> lt; // [neighbor_id -> link_state i.e timer]
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> ft; // [dest -> via_dv]
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> dvt; // [neighbor_id -> neighbor_dvs] 
    // Data Plane
    private ReentrantLock dphlock;
    private ReentrantLock uctlock;
    private ReentrantLock dctlock;
    private ConcurrentLinkedQueue<ConcurrentHashMap<byte[],byte[]>> buff_c; // buffer contents [digest -> payload]
    private ConcurrentHashMap<byte[],ConcurrentHashMap<Integer,Integer>> buff_s; // buffer states [digest -> dest -> flag]
    private ConcurrentHashMap<String,ConcurrentHashMap<Integer,byte[]>> buff_f; // received pkts to generate complete file
    // Testing
    private Message cm;
    private Tstat debug;
    private Tstat cstats;
    // TODO: private Network dtnet;
    
    public Router(String p, String i) throws IOException {
        // Control Plane
        this.ucount = 0;
        this.dcount = 0;
        this.dflag = true;
        this.hflag = true;
        // TODO: this.dtnet = null;
        this.id = Integer.parseInt(i);
        this.server = new ServerSocket(Integer.parseInt(p));
        this.lt = new ConcurrentHashMap<Integer, Neighbor>();
        this.ft = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>>();
        this.dvt = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>>();
        // Data Plane
        this.dphlock = new ReentrantLock();
        this.uctlock = new ReentrantLock();
        this.dctlock = new ReentrantLock();
        this.path = this.path + "/src/app/Test/data/" + this.id;
        this.buff_c = new ConcurrentLinkedQueue<ConcurrentHashMap<byte[],byte[]>>();
        this.buff_s = new ConcurrentHashMap<byte[],ConcurrentHashMap<Integer,Integer>>();
        this.buff_f = new ConcurrentHashMap<String,ConcurrentHashMap<Integer,byte[]>>();
        
        // Testing
        this.debug = new Tstat(this.id);
        this.cstats = new Tstat(this.id);
        // Server-Context
        new Thread(() -> {
            while (true) {
                if (server.isClosed())break;
                try {
                    // listen for incoming connections
                    Socket fromClient = server.accept(); 
                    // multi-threaded Server
                    new Thread(new Server(fromClient, this)).start();
                } catch (IOException e) {
                    printException(e);
                }
            }
        }).start();
    }

    // getters

    public Integer getID() {
        return this.id;
    }
    public Tstat getCstats(){
        return cstats;
    }
    public Tstat getDCstats(){
        return debug;
    }
    public Integer getUCount(){
        return ucount;
    }
    public Integer getDCount(){
        return dcount;
    }
    public Message getCMessage(){
        return cm;
    }
    public ServerSocket getServer(){
        return server;
    }
    public ReentrantLock getDPHLock(){
        return this.dphlock;
    }
    public static Integer getPort(Integer id) {
        return port_min + (id - id_min);
    }
    public static Integer getID(Integer port) {
        return id_min + (port - port_min);
    }
    public ConcurrentHashMap<Integer, Neighbor> getLT() {
        return this.lt;
    }
    public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> getFT() {
        return this.ft;
    }
    public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> getDVT() {
        return this.dvt;
    }
    public ConcurrentLinkedQueue<ConcurrentHashMap<byte[],byte[]>> getBuffContent() {
        return this.buff_c;
    }
    public ConcurrentHashMap<String,ConcurrentHashMap<Integer,byte[]>> getFileBuff() {
        return this.buff_f;
    }
    public ConcurrentHashMap<byte[], ConcurrentHashMap<Integer, Integer>> getBuffState() {
        return this.buff_s;
    }

    // methods

    public void setDataFlag(){
        dflag = true;
    }
    public void unsetDataFlag(){
        dflag = false;
    }
    public void unsetHelloFlag(){
        hflag = false;
    }
    public void setUCount(Integer uc){
        uctlock.lock();
        ucount = uc;
        uctlock.unlock();
    }
    public void setDCount(Integer dc){
        dctlock.lock();
        dcount = dc;
        dctlock.unlock();
    }
    public void setCMessage(Message m){
        cm = m;
    }
    public static String translateID(Integer i){
        return ("R" + (i-10));
    }
    public void printException(Exception e) {
        String trace = e.getMessage() + " - "; 
        StackTraceElement[] stack = e.getStackTrace();
        for(StackTraceElement line : stack){trace += line.toString();}
        Logger.getLogger("ErrorLog").warning(trace);
    }
    public void printExceptionWithNeighbor(Exception e, String rid, String nid) {
        String trace = rid + " - " +  nid + " - "+ e.getMessage() + " "; 
        StackTraceElement[] stack = e.getStackTrace();
        for(StackTraceElement line : stack){trace += line.toString();}
        Logger.getLogger("ErrorLog").warning(trace);
    }
    public byte[] combine(byte[] a, byte[] b){
        byte[] c = new byte[a.length+b.length];
        System.arraycopy(a,0,c,0,a.length);
        System.arraycopy(b,0,c,a.length,b.length);
        return c;
    }
    public void add_LT(Integer i, Socket l, Integer l_c, Integer l_s, InputStream c_r, OutputStream c_w) {
        // insert entry in lt
        Long c_t = Instant.now().toEpochMilli();
        Thread l_m = new Thread(new LinkFailureHandler(this, i));
        Neighbor n = new Neighbor(i, l, l_c, l_s, l_m, c_t, c_t, c_r, c_w);
        this.lt.put(i, n);
        // initialze dvt entry for i
        if (!this.dvt.containsKey(i)) {
            ConcurrentHashMap<Integer, Vector> dv = new ConcurrentHashMap<Integer, Vector>();
            dv.put(i, new Vector(l_c, l_s));
            this.dvt.put(i, dv);
        }
        // initiate link manager
        l_m.start();
    }
    // Client-Context
    public void addLink(String ip, Integer port, Integer cost) {
        try {
            Socket toServer = new Socket(ip, port); // connection request
            // Multi-Threaded Client
            new Thread(new Client(toServer, this, cost)).start();
        } catch (Exception e) {
            printException(e);
        }
    }
    public void send(OutputStream out, Message m) {
        String rn = "";
        Integer sno, type;
        synchronized(this){                      
            // TODO: synchronizing use of OutputStream and TStat across multiple neighbor threads 
            try {
                type = m.getType();
                /* TODO:
                if(dtnet != null && type != 6){  // increment sequence
                    dtnet.getSequenceLock().lock();
                    sno = dtnet.getSequence();
                    dtnet.setSequence(sno+1);
                    m.setSequence(sno+1);
                    dtnet.getSequenceLock().unlock();
                }*/
                switch (type) {
                    case 1:
                        SYN s = (SYN) m;
                        rn = Router.translateID(s.getSender());
                        out.write(s.writeMessage().array());
                        out.flush();
                        debug.countPkt(0);
                        debug.countMessages(1, m, Instant.now().toEpochMilli(), Instant.now().toEpochMilli());
                        break;
                    case 2:
                        SYN_ACK sa = (SYN_ACK) m;
                        rn = Router.translateID(sa.getSender());
                        out.write(sa.writeMessage().array());
                        out.flush();
                        debug.countPkt(1);
                        debug.countMessages(2, m, Instant.now().toEpochMilli(), Instant.now().toEpochMilli());
                        break;
                    case 3:
                        FIN f = (FIN) m;
                        rn = Router.translateID(f.getSender());
                        out.write(f.writeMessage().array());
                        out.flush();
                        debug.countPkt(2);
                        debug.countMessages(3, m, Instant.now().toEpochMilli(), Instant.now().toEpochMilli());
                        break;
                    case 4:
                        FIN_ACK fa = (FIN_ACK) m;
                        rn = Router.translateID(fa.getSender());
                        out.write(fa.writeMessage().array());
                        out.flush();
                        debug.countPkt(3);
                        debug.countMessages(4, m, Instant.now().toEpochMilli(), Instant.now().toEpochMilli());
                        break;
                    case 5:
                        UPDATE u = (UPDATE) m;
                        rn = Router.translateID(u.getSender());
                        out.write(u.writeMessage().array());
                        out.flush();
                        debug.countPkt(4);
                        debug.countMessages(5, m, Instant.now().toEpochMilli(), Instant.now().toEpochMilli());
                        break;
                    case 6:
                        if(hflag){
                            HELLO h = (HELLO) m;
                            rn = Router.translateID(h.getSender());
                            out.write(h.writeMessage().array());
                            out.flush();
                        }
                        break;
                    case 7:
                        if(dflag){
                            DATA_SUMMARY ds = (DATA_SUMMARY) m;
                            rn = Router.translateID(ds.getSender());
                            out.write(ds.writeMessage().array());
                            out.flush();
                        }
                        break;
                    case 8:
                        if(dflag){
                            DATA_PAYLOAD dp = (DATA_PAYLOAD) m;
                            rn = Router.translateID(dp.getSender());
                            out.write(dp.writeMessage().array());
                            out.flush();
                        }
                        break;
                }
            } catch (Exception e) {
                printExceptionWithNeighbor(e,Router.translateID(id),rn);
                if(!(e.toString().contains("Broken pipe") || e.toString().contains("Socket closed"))){
                    e.printStackTrace();
                }
            }
        }
    }
    public Message receive(InputStream in) throws IOException {
        Message m;
        ByteBuffer a, b;
        Integer size, type;
        byte[] sstream, dstream;
        m = new Message();
        sstream = new byte[4];
        if(!server.isClosed()){
            in.read(sstream);
            a = ByteBuffer.wrap(sstream);
            size = a.getInt() - 4;
            if(size > 0){
                dstream = new byte[size];
                in.read(dstream);
                dstream = combine(sstream, dstream);
                b = ByteBuffer.wrap(dstream);
                size = b.getInt();
                type = b.getInt();
                switch (type) {
                    case 1:
                        m = new SYN();
                        m.readMessage(b);
                        // m.printMessage();
                        break;
                    case 2:
                        m = new SYN_ACK();
                        m.readMessage(b);
                        // m.printMessage();
                        break;
                    case 3:
                        m = new FIN();
                        m.readMessage(b);
                        // m.printMessage();
                        break;
                    case 4:
                        m = new FIN_ACK();
                        m.readMessage(b);
                        // m.printMessage();
                        break;
                    case 5:
                        m = new UPDATE();
                        m.readMessage(b);
                        // m.printMessage();
                        break;
                    case 6:
                        m = new HELLO();
                        m.readMessage(b);
                        break;
                    case 7:
                        m = new DATA_SUMMARY();
                        m.readMessage(b);
                        // m.printMessage();
                        break;
                    case 8:
                        m = new DATA_PAYLOAD();
                        m.readMessage(b);
                        // m.printMessage();
                        break;
                }
            }
        }
        return m;
    }
    public void flushFileBuff(){
        for(String s:buff_f.keySet())buff_f.remove(s);
        for(byte[] b:buff_s.keySet())buff_s.remove(b);
        buff_c = new ConcurrentLinkedQueue<ConcurrentHashMap<byte[],byte[]>>();
    }
    public void restoreLinks(String[] links) throws InterruptedException {
        Integer ind = 0;
        String ip = "";
        Integer port = -1;
        Integer cost = -1;
        String[] curr_link = Arrays.copyOfRange(links, ind, ind+3);
        while(ind != links.length){
            ip = "localhost";
            cost =  Integer.parseInt(curr_link[2]);
            port = getPort(Integer.parseInt(curr_link[1]));
            addLink(ip, port, cost);
            TimeUnit.MILLISECONDS.sleep(500);
            if(lt.containsKey(getID(port))){
                ind = ind + 3;
                curr_link = Arrays.copyOfRange(links, ind, ind+3);
            }
        }
        System.out.println("\nBSDVRP - Connections established for "+translateID(id));
    }
    public void disconnect(String[] nid) throws InterruptedException, IOException {
        if(nid.length == 1 && nid[0].equals("ALL")){
            for (Integer n : this.lt.keySet()) {
                if (this.lt.get(n).getLinkState() == 1) {
                    Message m = new FIN(3, this.id, n);
                    OutputStream out = this.lt.get(n).getWrite(n);
                    send(out, m);
                    TimeUnit.MILLISECONDS.sleep(500);
                }
            }
        }else if(nid.length >= 1){
            for(String s:nid){
                Integer l = Integer.parseInt(s);
                if(this.lt.containsKey(l)){
                    if (this.lt.get(l).getLinkState() == 1) {
                        Message m = new FIN(3, this.id, l);
                        OutputStream out = this.lt.get(l).getWrite(l);
                        send(out, m);
                        TimeUnit.MILLISECONDS.sleep(500);
                    } 
                }
            }
        }
    }
    public void reconnect(String[] nid) throws InterruptedException, IOException {
        Integer l, port, cost;
        String ip = "localhost";
        if(nid.length == 1 && nid[0].equals("ALL")){
            for (Integer n : this.lt.keySet()) {
                if(this.lt.get(n).getLinkState() == 0){
                    cost = this.lt.get(n).getLinkCost();
                    port = getPort(n);
                    addLink(ip, port, cost);
                    TimeUnit.MILLISECONDS.sleep(500);
                }
            }
        }else if(nid.length >= 1){
            for(String s:nid){
                l = Integer.parseInt(s);
                if(this.lt.containsKey(l)){
                    if(this.lt.get(l).getLinkState() == 0){
                        cost = this.lt.get(l).getLinkCost();
                        port = getPort(l);
                        addLink(ip, port, cost);
                        TimeUnit.MILLISECONDS.sleep(500);
                    }
                }
            }
        }
    }
    public void terminateRouter() throws IOException, InterruptedException{
        String[] in = {"ALL"};
        flushFileBuff(); // flushing data buffers
        disconnect(in);  // closing open connections
        server.close();  // closing listening port
        System.out.println(Router.translateID(id) + " terminated !");
    }

}
