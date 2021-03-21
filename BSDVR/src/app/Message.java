package app;

import java.nio.ByteBuffer;

public class Message{
    private Integer size;
    private Integer type;
    private Integer seq;
    private String[] types = {"SYN", "SYN-ACK", "FIN", "FIN-ACK", 
                              "UPDATE", "HELLO", "DATA_SUMMARY", "DATA_PAYLOAD"};
    public Message(){
        this.size = -1;
        this.type = -1;
        this.seq = 0;
    }
    //getters
    public Integer getType(){return this.type;}
    public Integer getSize(){return this.size;}
    public Integer getSequence(){return this.seq;}
    public String[] getTypes(){return this.types;}
    //setters
    public void setType(Integer t){this.type = t;}
    public void setSize(Integer s){this.size = s;}
    public void setSequence(Integer s){this.seq = s;}
    //methods
    public void readMessage(ByteBuffer mess){
        mess.rewind();
        this.size = mess.getInt();
        this.type = mess.getInt();
        this.seq = mess.getInt();
    }
    public ByteBuffer writeMessage(ByteBuffer b){
        b.putInt(this.size);
        b.putInt(this.type);
        b.putInt(this.seq);
        return b;
    }
    public Boolean isZero(){
        if(size.equals(0) || size.equals(-1) || type.equals(0)||type.equals(-1)){
            return true;
        }
        return false;
    }
    public void printMessage(){
        //System.out.print("\n\n{size: "+size+", type: " + types[type-1]+", #seq: " +seq);
        System.out.print("\n\n{#seq: "+seq+", size: " + size+", type: " +types[type-1]);
    }
    public String toString(){
        //return "{size: "+size+", type: " + types[type-1]+", #seq: " +seq;
        return "{#seq: "+seq+", size: " + size+", type: " +types[type-1];
    }
}