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
        sender = -1;
        receiver = -1;
        dest = -1;
        total_chunks = -1;
        chunk_id = -1;
        size = -1;
        nhops = -1;
        file = null;
        digest = null;
        payload = null;
    }
    public DATA_PAYLOAD(Integer t, Integer s, Integer r, Integer d, Integer tc, Integer ci,
                        Integer si, Integer nh, byte[] fi, byte[] di,byte[] pa){
        super.setSize(MTU);
        super.setType(t);
        sender = s;
        receiver = r;
        dest = d;
        total_chunks = tc;
        chunk_id = ci;
        size = si;
        nhops = nh;
        file = fi;
        digest = di;
        payload = pa;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return sender;}
    public Integer getReceiver(){return receiver;}
    public Integer getDestination(){return dest;}
    public Integer getTotalChunks(){return total_chunks;}
    public Integer getChunkID(){return chunk_id;}
    public Integer getSize(){return size;}
    public Integer getHops(){return nhops;}
    public byte[] getFile(){return file;}
    public byte[] getDigest(){return digest;}
    public byte[] getPayload(){return payload;}
    //setters
    public void setHops(Integer n){nhops = n;}
    public void setSender(Integer s){sender = s;}
    public void setReceiver(Integer r){receiver = r;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        sender = mess.getInt();
        receiver = mess.getInt();
        dest = mess.getInt();
        total_chunks = mess.getInt();
        chunk_id = mess.getInt();
        size = mess.getInt();
        nhops = mess.getInt();
        file = new byte[DIGEST_SIZE];
        mess.get(file);
        digest = new byte[DIGEST_SIZE];
        mess.get(digest);
        payload = new byte[MTU - (2 * DIGEST_SIZE) - 40];
        mess.get(payload);
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(MTU);
        super.writeMessage(b);
        b.putInt(sender);
        b.putInt(receiver);
        b.putInt(dest);
        b.putInt(total_chunks);
        b.putInt(chunk_id);
        b.putInt(size);
        b.putInt(nhops);
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
        System.out.println(", sender: " +Router.translateID(sender)+
                           ", receiver: " +Router.translateID(receiver)+ 
                           ", destination: "+Router.translateID(dest)+
                           ", total_chunks: "+Integer.toString(total_chunks)+
                           ", chunk_id: "+Integer.toString(chunk_id)+
                           ", size: "+Integer.toString(size)+
                           ", nhops: "+Integer.toString(nhops)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(sender)
                                + ", receiver: " +Router.translateID(receiver)
                                + ", destination: "+Router.translateID(dest)
                                + ", total_chunks: "+Integer.toString(total_chunks)
                                + ", chunk_id: "+Integer.toString(chunk_id)
                                + ", size: "+Integer.toString(size)+"}";
                                // + ", nhops: "+Integer.toString(nhops)+"}";
    }
}