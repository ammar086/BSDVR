package app;

import java.nio.ByteBuffer;

public class SYN_ACK extends Message{
    private Integer sender;
    private Integer receiver;
    private Integer cost;

    public SYN_ACK(){
        sender = -1;
        receiver = -1;
        cost = -1;
    }
    
    public SYN_ACK(Integer t, Integer s, Integer r,Integer c){
        super.setSize(24);
        super.setType(t);
        sender = s;
        receiver = r;
        cost = c;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return sender;}
    public Integer getReceiver(){return receiver;}
    public Integer getCost(){return cost;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        sender = mess.getInt();
        receiver = mess.getInt();
        cost = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(24);
        super.writeMessage(b);
        b.putInt(sender);
        b.putInt(receiver);
        b.putInt(cost);
        return b;
    }
    public Boolean isZero(){
        if(sender.equals(0) || receiver.equals(0) || cost.equals(0) || super.isZero()){
            return true;
        }
        return false;
    }
    public void printMessage(){
        super.printMessage();
        System.out.println(", sender: " +Router.translateID(sender)+
                           ", receiver: " +Router.translateID(receiver)+ 
                           ", cost: "+Integer.toString(cost)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(sender)
                                + ", receiver: " +Router.translateID(receiver) 
                                + ", cost: "+Integer.toString(cost)+"}";
    }
}