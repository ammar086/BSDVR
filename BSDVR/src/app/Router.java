package app;

import java.io.File;
import java.net.Socket;
import java.util.Arrays;
import java.time.Instant;
import java.util.Iterator;
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
    public String path = System.getProperty("user.dir");
    // Control Plane
    private Integer id;
    private Boolean dflag;
    private Boolean rflag;
    private Boolean hflag;
    private Integer ucount;
    private Integer dcount;
    private ServerSocket server;
    private ConcurrentHashMap<Integer, Neighbor> lt; // [neighbor_id -> link_state i.e timer]
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> ft; // [dest -> via_dv]
    private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> dvt; // [neighbor_id -> neighbor_dvs] 
    private PendingReplyQueue prq;
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
        ucount = 0;
        dcount = 0;
        dflag = true;
        rflag = true;
        hflag = false;
        // TODO: dtnet = null;
        id = Integer.parseInt(i);
        server = new ServerSocket(Integer.parseInt(p));
        lt = new ConcurrentHashMap<Integer, Neighbor>();
        ft = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>>();
        dvt = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>>();
        prq = new PendingReplyQueue(this);
        // Data Plane
        dphlock = new ReentrantLock();
        uctlock = new ReentrantLock();
        dctlock = new ReentrantLock();
        path = path + "/src/app/Test/data/" + id + "/";
        buff_c = new ConcurrentLinkedQueue<ConcurrentHashMap<byte[],byte[]>>();
        buff_s = new ConcurrentHashMap<byte[],ConcurrentHashMap<Integer,Integer>>();
        buff_f = new ConcurrentHashMap<String,ConcurrentHashMap<Integer,byte[]>>();
        
        // Testing
        debug = new Tstat(id);
        cstats = new Tstat(id);
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
        // Pending Reply Queue
        new Thread(prq).start();
    }

    // getters
    public Integer getID() {
        return id;
    }
    public Tstat getCstats(){
        return cstats;
    }
    public Tstat getDCstats(){
        return debug;
    }
    public Boolean getRFlag(){
        return rflag;
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
        return dphlock;
    }
    public PendingReplyQueue getPRQ(){
        return prq;
    }
    public static Integer getPort(Integer id) {
        return Constants.PORT_MIN + (id - Constants.ID_MIN);
    }
    public static Integer getID(Integer port) {
        return Constants.ID_MIN + (port - Constants.PORT_MIN);
    }
    public ConcurrentHashMap<Integer, Neighbor> getLT() {
        return lt;
    }
    public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> getFT() {
        return ft;
    }
    public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Vector>> getDVT() {
        return dvt;
    }
    public ConcurrentLinkedQueue<ConcurrentHashMap<byte[],byte[]>> getBuffContent() {
        return buff_c;
    }
    public ConcurrentHashMap<String,ConcurrentHashMap<Integer,byte[]>> getFileBuff() {
        return buff_f;
    }
    public ConcurrentHashMap<byte[], ConcurrentHashMap<Integer, Integer>> getBuffState() {
        return buff_s;
    }

    // methods
    public void setRFlag(){
        rflag = true;
    }
    public void unsetRFlag(){
        rflag = false;
    }
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
        lt.put(i, n);
        // initialze dvt entry for i
        if (!dvt.containsKey(i)) {
            ConcurrentHashMap<Integer, Vector> dv = new ConcurrentHashMap<Integer, Vector>();
            dv.put(i, new Vector(l_c, l_s));
            dvt.put(i, dv);
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
        Integer /*sno,*/ type;        
        // synchronizing use of TStat across multiple neighbor threads 
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
                    debug.sendMessageUpate(1,m);
                    out.write(s.writeMessage().array());
                    out.flush();
                    break;
                case 2:
                    SYN_ACK sa = (SYN_ACK) m;
                    rn = Router.translateID(sa.getSender());
                    debug.sendMessageUpate(2,m);
                    out.write(sa.writeMessage().array());
                    out.flush();
                    break;
                case 3:
                    FIN f = (FIN) m;
                    rn = Router.translateID(f.getSender());
                    debug.sendMessageUpate(3,m);
                    out.write(f.writeMessage().array());
                    out.flush();
                    break;
                case 4:
                    FIN_ACK fa = (FIN_ACK) m;
                    rn = Router.translateID(fa.getSender());
                    debug.sendMessageUpate(4,m);
                    out.write(fa.writeMessage().array());
                    out.flush();
                    break;
                case 5:
                    UPDATE u = (UPDATE) m;
                    rn = Router.translateID(u.getSender());
                    debug.sendMessageUpate(5,m);
                    out.write(u.writeMessage().array());
                    out.flush();
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
                        break;
                    case 2:
                        m = new SYN_ACK();
                        m.readMessage(b);
                        break;
                    case 3:
                        m = new FIN();
                        m.readMessage(b);
                        break;
                    case 4:
                        m = new FIN_ACK();
                        m.readMessage(b);
                        break;
                    case 5:
                        m = new UPDATE();
                        m.readMessage(b);
                        break;
                    case 6:
                        m = new HELLO();
                        m.readMessage(b);
                        break;
                    case 7:
                        m = new DATA_SUMMARY();
                        m.readMessage(b);
                        break;
                    case 8:
                        m = new DATA_PAYLOAD();
                        m.readMessage(b);
                        break;
                }
            }
        }
        return m;
    }
    public void flushFileBuff(){
        for(String s:buff_f.keySet())buff_f.remove(s);
        for(byte[] b:buff_s.keySet())buff_s.remove(b);
        buff_c.clear();
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
            for (Integer n : lt.keySet()) {
                if (lt.get(n).getLinkState() == 1) {
                    Message m = new FIN(3, id, n);
                    OutputStream out = lt.get(n).getWrite(n);
                    send(out, m);
                    TimeUnit.MILLISECONDS.sleep(500);
                }
            }
        }else if(nid.length >= 1){
            for(String s:nid){
                Integer l = Integer.parseInt(s);
                if(lt.containsKey(l)){
                    if (lt.get(l).getLinkState() == 1) {
                        Message m = new FIN(3, id, l);
                        OutputStream out = lt.get(l).getWrite(l);
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
            for (Integer n : lt.keySet()) {
                if(lt.get(n).getLinkState() == 0){
                    cost = lt.get(n).getLinkCost();
                    port = getPort(n);
                    addLink(ip, port, cost);
                    TimeUnit.MILLISECONDS.sleep(500);
                }
            }
        }else if(nid.length >= 1){
            for(String s:nid){
                l = Integer.parseInt(s);
                if(lt.containsKey(l)){
                    if(lt.get(l).getLinkState() == 0){
                        cost = lt.get(l).getLinkCost();
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
        unsetRFlag();    // deactivate pending reply queue
        server.close();  // closing listening port
        System.out.println(Router.translateID(id) + " terminated !");
    }
    public Integer getCurrentNextHopFT(Integer dest){
        if(ft.containsKey(dest)){
            return (Integer) ft.get(dest).keySet().toArray()[0];
        }else{
            return -1;
        }
    }
    public Boolean isValid_ID(Integer id){
        if(id >= Constants.ID_MIN && id <= Constants.ID_MAX){
            return true;
        }else{
            return false;
        }
    }
    public void removeFakePaths(Integer sender, Integer dest, Vector new_vec){
        Integer curr_state, curr_next_hop;
        ArrayList<Integer> dests = new ArrayList<Integer>();
        for(Integer dst:ft.keySet()){
            curr_next_hop = getCurrentNextHopFT(dst);
            curr_state = ft.get(dst).get(curr_next_hop).getState();
            if(curr_state == 1 && new_vec.getState() == 0){
                if((sender == curr_next_hop && dest == dst)){
                    dests.add(dst);
                }
                if(sender == dest && lt.get(dest).getLinkState() == 0){
                    if (curr_next_hop == sender && dest != dst){
                        dests.add(dst);
                    }
                }
            }
        }
        for (Integer m : dvt.keySet()) { // Neighbors
            for (Integer n : dvt.get(m).keySet()) { // Destinations
                for(Integer x : dests){
                    if(n == x){
                        if(m != n){
                            curr_next_hop = getCurrentNextHopFT(n);
                            if(m != curr_next_hop)dvt.get(m).remove(n);
                        }
                    }
                }
            }
        }
    }
    public synchronized void updateDVT(Integer sender/*via*/, Integer dest, Vector new_vec/*nvec*/){
        Integer link_cost, curr_cost, total_cost;
        // resolve updates via possible fake paths
        // TODO: synchronizing access of DVT and FT across multiple neighbor threads
        if (ft.containsKey(dest)) {
            try {
                removeFakePaths(sender, dest, new_vec);
            } catch (Exception e) {
                printException(e);
            }
        }
        // resolve normal updates
        if(lt.containsKey(sender) && dvt.containsKey(sender)){      // TODO: revist use of this validation
            link_cost = lt.get(sender).getLinkCost();
            if (dest == sender) {
                dvt.get(sender).put(dest, new_vec);
                lt.get(sender).setLinkCost(new_vec.getCost());
                for (Integer n : dvt.get(sender).keySet()){         // update dvt entries for via
                    if(dvt.get(sender).containsKey(n)){
                        curr_cost = dvt.get(sender).get(n).getCost();
                        dvt.get(sender).get(n).setState(new_vec.getState());
                        dvt.get(sender).get(n).setCost(curr_cost - link_cost + new_vec.getCost());
                    }
                }
            } else {
                if (new_vec.getCost() == Integer.MAX_VALUE) {
                    dvt.get(sender).put(dest, new_vec);
                } else {
                    total_cost = link_cost + new_vec.getCost();
                    new_vec.setCost(total_cost);
                    dvt.get(sender).put(dest, new_vec);
                }
            }
        }
    }
    public Boolean isBetter(Vector new_vec, Vector curr_vec) {
        if (new_vec.getState() == 1) {
            if (curr_vec.getState() == 1) {
                return (curr_vec.getCost() <= new_vec.getCost()) ? false : true; // new_vec -- active, curr_vec -- active
            }
            return (new_vec.getCost() < Constants.THRESHOLD_COST) ? true : false; // new_vec -- active, curr_vec -- inactive
        } else {
            if (curr_vec.getState() == 1) {
                return (curr_vec.getCost() <= Constants.THRESHOLD_COST) ? false : true; // new_vec -- inactive, curr_vec -- active
            }
            return (curr_vec.getCost() <= new_vec.getCost()) ? false : true; // new_vec -- inactive, curr_vec -- inactive
        }
    }
    public void refreshFT(Integer dest, Integer sender) {
        Vector v;
        Integer curr_next_hop;
        ConcurrentHashMap<Integer, Vector> dv = new ConcurrentHashMap<Integer, Vector>();
        if(dvt.containsKey(sender) && dvt.get(sender).containsKey(dest)){
            v = new Vector(dvt.get(sender).get(dest));          // update FT entry for curr_next_hop from DVT
            dv.put(sender, v);
            ft.put(dest, dv);
        }else{                                                  // entry removed in DVT
            curr_next_hop = getCurrentNextHopFT(dest);
            ft.get(dest).get(curr_next_hop).setState(0);
        } 
    }
    public synchronized ArrayList<Integer> computeFT() {
        Integer curr_next_hop;
        Vector old_vec, curr_vec, new_vec;
        ArrayList<Integer> changes = new ArrayList<Integer>();
        // TODO: synchronizing access of DVT and FT across multiple neighbor threads again
        for (Integer m : dvt.keySet()) { // Neighbors
            for (Integer n : dvt.get(m).keySet()) { // Destinations
                ConcurrentHashMap<Integer, Vector> dv = new ConcurrentHashMap<Integer, Vector>();
                if (ft.keySet().contains(n)) {                              // entry already exists -- apply precedence rule
                    try {
                        curr_next_hop = getCurrentNextHopFT(n);
                        if(isValid_ID(n)){
                            if(ft.get(n).containsKey(curr_next_hop)){
                                old_vec = new Vector(ft.get(n).get(curr_next_hop));
                                refreshFT(n, curr_next_hop);                // apply update (if any) to current entry from dvt
                                new_vec = new Vector(dvt.get(m).get(n));
                                curr_vec = new Vector(ft.get(n).get(curr_next_hop));
                                if (isBetter(new_vec, curr_vec)) {
                                    dv.put(m, new_vec);
                                    ft.replace(n, dv);
                                    if (!changes.contains(n)) {
                                        changes.add(n);
                                    }
                                } else if (!curr_vec.getCost().equals(old_vec.getCost()) || !curr_vec.getState().equals(old_vec.getState())) {
                                    if (!changes.contains(n)) {
                                        changes.add(n);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        printException(e);
                        // TODO: Is this continue necessary?
                        // continue;
                    }
                } else { // entry doesnot exist -- append
                    new_vec = new Vector(dvt.get(m).get(n));
                    dv.put(m, new_vec);
                    ft.put(n, dv);
                    changes.add(n);
                }
            }
        }
        changes.remove(getID());
        return changes;
    }
    // TODO: Adding data plane and application functionalites to router [application layer coupled]
    // reads data file and prepares it for transmission
    // application layer function
    public static byte[] readFile(String path) throws IOException {
        File load_file = new File(path);
        Long f_size = load_file.length();
        FileInputStream fis = new FileInputStream(load_file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        byte[] f_bytes = new byte[f_size.intValue()];
        bis.read(f_bytes, 0, f_size.intValue());
        if (bis != null)
            bis.close();
        return f_bytes;
    }
    // divides data file bytes into smaller chuncks (MTU) for transmission
    // application layer function
    public ArrayList<byte[]> generateChunks(byte[] file) {
        ArrayList<byte[]> pkts = new ArrayList<byte[]>();
        Integer start, end, size;
        start = 0;
        size = Constants.MTU - (2 * Constants.DIGEST_SIZE) - 40;
        while (start < file.length) {
            end = Math.min(file.length, start + size);
            pkts.add(Arrays.copyOfRange(file, start, end));
            start += size;
        }
        return pkts;
    }
    // adds received data packet into a list to reconstruct the received file
    // application layer function
    public void addFileChunk(Message m){
        String file_id;
        DATA_PAYLOAD dp;
        Integer total_chunks, chunk_id;
        ConcurrentHashMap<Integer,byte[]> chunk;
        dp = (DATA_PAYLOAD)m;
        chunk_id = dp.getChunkID();
        total_chunks = dp.getTotalChunks();
        file_id = new String(dp.getFile());
        chunk = new ConcurrentHashMap<Integer, byte[]>();
        chunk.put(chunk_id,dp.writeMessage().array());
        if(buff_f.containsKey(file_id)){
            // note: duplication prevented by use of digests
            if(buff_f.get(file_id).keySet().size() != total_chunks){
                buff_f.get(file_id).put(chunk_id,dp.writeMessage().array());
            }
        }else{
            buff_f.put(file_id,chunk);
        }
    }
    // creates SHA_256 digests of data payloads for epidemic style routing
    // data plane function
    public byte[] generateDigest(byte[] chunk, Integer sender, Integer receiver) throws NoSuchAlgorithmException {
        MessageDigest algo = MessageDigest.getInstance(Constants.DEFAULT_DIGEST_ALGO);
        byte[] mess_digest = Arrays.copyOfRange(chunk, 0, chunk.length+8);
        byte[] src = ByteBuffer.allocate(4).putInt(sender).array();
        byte[] dst = ByteBuffer.allocate(4).putInt(receiver).array();
        System.arraycopy(src,0,mess_digest,chunk.length,4);
        System.arraycopy(dst,0,mess_digest,chunk.length+4,4);
        return algo.digest(mess_digest);
    }
    // updates flag for data packets in the buffer for applyting drop policy
    // data plane function
    public void updateFlag(byte[] digest, Integer flag){
        Integer dst;
        try {
            dst = (Integer) buff_s.get(digest).keySet().toArray()[0];
            buff_s.get(digest).put(dst,flag);
        } catch (Exception e) {
            printException(e);
        }
    }
    // retrieves summary vector for a given destination from all data packets in the buffer
    // data plane function
    public ArrayList<byte[]> getSummary(Integer destination){
        Integer next_hop, curr_dest, pkt_state;
        ArrayList<byte[]> summary = new ArrayList<byte[]>();
        for(byte[] digest: buff_s.keySet()){
            try {
                curr_dest = (Integer) buff_s.get(digest).keySet().toArray()[0];
                pkt_state = (Integer) buff_s.get(digest).get(curr_dest);
                // only retreive summary vector for non-forwarded and inactive-forwarded packets
                if(ft.containsKey(curr_dest) && pkt_state != 2){
                    next_hop = getCurrentNextHopFT(curr_dest);
                    if(destination.equals(curr_dest)){ 
                        // destination-based [multiple-copy case]
                        if(destination.equals(next_hop)){
                            if(pkt_state == 0 || pkt_state == 1){
                                summary.add(digest); 
                                // TODO: is this continue necessary?
                                // continue;
                            }
                        }else{
                        // next-hop-based [single-copy case]
                            if(pkt_state == 0){
                                summary.add(digest);
                                // TODO: is this continue also necessary?
                                // continue;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                printException(e);
                continue;
            }
        }
        return summary;
    }
    // sends DATA_SUMMARY to a given destination if its data packets are available in a router's buffer
    // data plane function
    public void sendSummary(ArrayList<byte[]> summary, Integer destination, Integer mode){
        Message m;
        byte[] summary_vec;
        Integer num_vec, state;
        OutputStream out;
        try {
            state = lt.get(destination).getLinkState();
            // next-hop reachable
            if(state == 1){ 
                num_vec = summary.size();
                summary_vec = new byte[0];
                for(byte[] digest: summary){summary_vec = combine(summary_vec, digest);}
                if(summary_vec.length > 0){
                    out = lt.get(destination).getWrite(destination);
                    m = new DATA_SUMMARY(7,id,destination,mode,num_vec, summary_vec);
                    send(out,m);
                }
            }
        } catch (Exception e) {
            printException(e);
        }
    }
    // drop data packets from buffer based on their forwarding status
    // data plane function
    public void DropPolicy(){
        byte[] digest, curr_pkt;
        ArrayList<byte[]> summary;
        Integer state,dest, next_hop;
        Iterator<ConcurrentHashMap<byte[],byte[]>> iter;
        ConcurrentHashMap<byte[],byte[]> pkt,old_active_forwarded,old_inactive_forwarded;
        // attempt to forward packets before removing them
        try {
            dphlock.lock();
            if(buff_c.size() > 0){
                for(Integer dst: ft.keySet()){
                    summary = getSummary(dst);
                    next_hop = getCurrentNextHopFT(dst);
                    sendSummary(summary,next_hop,0);
                }
            }
            dphlock.unlock();
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (Exception e) {
            printException(e);
        }
        // apply drop policy of removing old atice forwarded packets before
        // old inactive forwared packets
        iter = buff_c.iterator();
        old_inactive_forwarded= new ConcurrentHashMap<byte[],byte[]>();
        old_active_forwarded = new ConcurrentHashMap<byte[],byte[]>();
        while(iter.hasNext()){
            try {
                pkt = iter.next();
                digest = (byte[]) pkt.keySet().toArray()[0];
                curr_pkt = (byte[]) pkt.get(digest);
                dest = (Integer) buff_s.get(digest).keySet().toArray()[0];
                state = buff_s.get(digest).get(dest);
                // finding oldest forwarded packets
                if(state == 1 && old_inactive_forwarded.size() == 0){ 
                    // oldest inactive-forwarded
                    old_inactive_forwarded.put(digest,curr_pkt);
                }else if(state == 2 && old_active_forwarded.size() == 0){  
                    // oldest active-forwarded
                    old_active_forwarded.put(digest,curr_pkt);
                }
            } catch (Exception e) {
                printException(e);
                continue;
            }
        }
        if(old_active_forwarded.size() != 0 || old_inactive_forwarded.size() != 0){
            if(old_active_forwarded.size() != 0){
                digest = (byte[]) old_active_forwarded.keySet().toArray()[0];
                buff_s.remove(digest);
                buff_c.remove(old_active_forwarded);
                System.out.println("Packet Dropped at "+translateID(id)+"\n");
            }else{
                digest = (byte[]) old_inactive_forwarded.keySet().toArray()[0];
                buff_s.remove(digest);
                buff_c.remove(old_inactive_forwarded);
                System.out.println("Packet Dropped at "+translateID(id)+"\n");
            }
        }
    }
    // adds new data packet chunks for transmission into the buffer
    // data plane function
    public void addBuffPkt(byte[] digest, byte[] content, Integer destination){
        ConcurrentHashMap<byte[],byte[]> payload  = new ConcurrentHashMap<byte[],byte[]>();
        ConcurrentHashMap<Integer,Integer> state = new ConcurrentHashMap<Integer,Integer>();
        payload.put(digest, content);
        state.put(destination,0);
        buff_c.add(payload);
        buff_s.put(digest,state);
    }
    // initiate tranmssion of a file in the data plane
    // application layer function
    public void transmitFile(String fname, String[] destinations){
        // System.out.println("\ninitiated transmission of \""+fname+"\" to destination(s): "+Arrays.toString(destinations)+" ...");
        new Thread(() -> {
            String file_tag, path;
            DATA_PAYLOAD dp;
            ArrayList<byte[]> pkts;
            Integer iter, dest_iter, curr_dest, total_chunks, chunk_id, chunk_size;
            byte[] content, digest, payload, file_id, file_bytes;
            DataPacketHandler data_pack = null;
            try {
                // path = path + "/send/" + fname;
                path = System.getProperty("user.dir") + "/src/app/Test/data/" + fname;
                path = path.replace("bin/", "");
                file_bytes = readFile(path);
                // generate chunks
                pkts = generateChunks(file_bytes);
                total_chunks = pkts.size();
                file_tag = id+"-"+fname;
                // file_id = fname + src_id
                file_id = Arrays.copyOfRange(file_tag.getBytes(), 0, Constants.DIGEST_SIZE);
                iter = 0;
                while(iter < total_chunks){
                    // generate data payload messages
                    dest_iter = 0;
                    while(dest_iter != destinations.length){
                        if(Constants.BUFFER_CAPACITY == buff_s.size()){DropPolicy();}
                        if((Constants.BUFFER_CAPACITY > buff_c.size())){
                            chunk_id = iter + 1;
                            chunk_size = pkts.get(iter).length;
                            curr_dest = Integer.parseInt(destinations[dest_iter]);
                            payload = Arrays.copyOf(pkts.get(iter), Constants.MTU - (2 * Constants.DIGEST_SIZE) - 40);
                            digest = generateDigest(payload,id,curr_dest);
                            dp = new DATA_PAYLOAD(8, -1, -1, curr_dest, total_chunks, chunk_id, chunk_size, 16, file_id, digest, payload);
                            content = dp.writeMessage().array();
                            addBuffPkt(digest, content, curr_dest);
                            dest_iter+=1; // next destination
                        }
                    }
                    iter+=1; // next chunk
                }
                if(data_pack == null){data_pack = new DataPacketHandler(this);}    
            } catch (Exception e) {
                printException(e);
            }
        }).start();
    }
}
