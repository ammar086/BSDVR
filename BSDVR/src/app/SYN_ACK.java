package app;

import java.nio.ByteBuffer;

public class SYN_ACK extends Message{
    private Integer sender;
    private Integer receiver;
    private Integer cost;

    public SYN_ACK(){
        this.sender = -1;
        this.receiver = -1;
        this.cost = -1;
    }
    
    public SYN_ACK(Integer t, Integer s, Integer r,Integer c){
        super.setSize(24);
        super.setType(t);
        this.sender = s;
        this.receiver = r;
        this.cost = c;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return this.sender;}
    public Integer getReceiver(){return this.receiver;}
    public Integer getCost(){return this.cost;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        this.sender = mess.getInt();
        this.receiver = mess.getInt();
        this.cost = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(24);
        super.writeMessage(b);
        b.putInt(this.sender);
        b.putInt(this.receiver);
        b.putInt(this.cost);
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
        System.out.println(", sender: " +Router.translateID(this.sender)+
                           ", receiver: " +Router.translateID(this.receiver)+ 
                           ", cost: "+Integer.toString(this.cost)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(this.sender)
                                + ", receiver: " +Router.translateID(this.receiver) 
                                + ", cost: "+Integer.toString(this.cost)+"}";
    }
}