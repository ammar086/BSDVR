package app;

import java.nio.ByteBuffer;

public class DATA_PAYLOAD extends Message {
    public static Integer MTU = 5000;
    public static Integer DIGEST_SIZE = 20;
    private Integer sender;
    private Integer receiver;
    private Integer dest;
    private Integer total_chunks;
    private Integer chunk_id;
    private Integer size;
    private Integer nhops;
    private byte[] file;
    private byte[] digest;
    private byte[] payload;

    public DATA_PAYLOAD(){
        this.sender = -1;
        this.receiver = -1;
        this.dest = -1;
        this.total_chunks = -1;
        this.chunk_id = -1;
        this.size = -1;
        this.nhops = -1;
        this.file = null;
        this.digest = null;
        this.payload = null;
    }
    public DATA_PAYLOAD(Integer t, Integer s, Integer r, Integer d, Integer tc, Integer ci,
                        Integer si, Integer nh, byte[] fi, byte[] di,byte[] pa){
        super.setSize(MTU);
        super.setType(t);
        this.sender = s;
        this.receiver = r;
        this.dest = d;
        this.total_chunks = tc;
        this.chunk_id = ci;
        this.size = si;
        this.nhops = nh;
        this.file = fi;
        this.digest = di;
        this.payload = pa;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return this.sender;}
    public Integer getReceiver(){return this.receiver;}
    public Integer getDestination(){return this.dest;}
    public Integer getTotalChunks(){return this.total_chunks;}
    public Integer getChunkID(){return this.chunk_id;}
    public Integer getSize(){return this.size;}
    public Integer getHops(){return this.nhops;}
    public byte[] getFile(){return this.file;}
    public byte[] getDigest(){return this.digest;}
    public byte[] getPayload(){return this.payload;}
    //setters
    public void setHops(Integer n){this.nhops = n;}
    public void setSender(Integer s){this.sender = s;}
    public void setReceiver(Integer r){this.receiver = r;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        this.sender = mess.getInt();
        this.receiver = mess.getInt();
        this.dest = mess.getInt();
        this.total_chunks = mess.getInt();
        this.chunk_id = mess.getInt();
        this.size = mess.getInt();
        this.nhops = mess.getInt();
        this.file = new byte[DIGEST_SIZE];
        mess.get(file);
        this.digest = new byte[DIGEST_SIZE];
        mess.get(digest);
        this.payload = new byte[MTU - (2 * DIGEST_SIZE) - 40];
        mess.get(payload);
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(MTU);
        super.writeMessage(b);
        b.putInt(this.sender);
        b.putInt(this.receiver);
        b.putInt(this.dest);
        b.putInt(this.total_chunks);
        b.putInt(this.chunk_id);
        b.putInt(this.size);
        b.putInt(this.nhops);
        b.put(file);
        b.put(digest);
        b.put(payload);
        return b;
    }
    public Boolean isZero(){
        if(sender.equals(0) || receiver.equals(0) || dest.equals(0) || total_chunks.equals(0) || 
            chunk_id.equals(0) || size.equals(0) || nhops.equals(0) || super.isZero()){
            return true;
        }
        return false;
    }
    public void printMessage(){
        super.printMessage();
        System.out.println(", sender: " +Router.translateID(this.sender)+
                           ", receiver: " +Router.translateID(this.receiver)+ 
                           ", destination: "+Router.translateID(this.dest)+
                           ", total_chunks: "+Integer.toString(this.total_chunks)+
                           ", chunk_id: "+Integer.toString(this.chunk_id)+
                           ", size: "+Integer.toString(this.size)+
                           ", nhops: "+Integer.toString(this.nhops)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(this.sender)
                                + ", receiver: " +Router.translateID(this.receiver)
                                + ", destination: "+Router.translateID(this.dest)
                                + ", total_chunks: "+Integer.toString(this.total_chunks)
                                + ", chunk_id: "+Integer.toString(this.chunk_id)
                                + ", size: "+Integer.toString(this.size)+"}";
                                // + ", nhops: "+Integer.toString(this.nhops)+"}";
    }
}