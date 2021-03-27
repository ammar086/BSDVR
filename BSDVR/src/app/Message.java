package app;

import java.nio.ByteBuffer;

public class Message{
    private Integer size;
    private Integer type;
    private Integer seq;
    private String[] types = {"SYN", "SYN-ACK", "FIN", "FIN-ACK", 
                              "UPDATE", "HELLO", "DATA_SUMMARY", "DATA_PAYLOAD"};
    public Message(){
        size = -1;
        type = -1;
        seq = 0;
    }
    //getters
    public Integer getType(){return type;}
    public Integer getSize(){return size;}
    public Integer getSequence(){return seq;}
    public String[] getTypes(){return types;}
    //setters
    public void setType(Integer t){type = t;}
    public void setSize(Integer s){size = s;}
    public void setSequence(Integer s){seq = s;}
    //methods
    public void readMessage(ByteBuffer mess){
        mess.rewind();
        size = mess.getInt();
        type = mess.getInt();
        seq = mess.getInt();
    }
    public ByteBuffer writeMessage(ByteBuffer b){
        b.putInt(size);
        b.putInt(type);
        b.putInt(seq);
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