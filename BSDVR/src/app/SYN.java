package app;

import java.nio.ByteBuffer;

public class SYN extends Message{
    private Integer sender;
    private Integer cost;

    public SYN(){
        sender = -1;
        cost = -1;
    }
    
    public SYN(Integer t, Integer s, Integer c){
        super.setSize(20);
        super.setType(t);
        sender = s;
        cost = c;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return sender;}
    public Integer getCost(){return cost;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        sender = mess.getInt();
        cost = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(20);
        super.writeMessage(b);
        b.putInt(sender);
        b.putInt(cost);
        return b;
    }
    public Boolean isZero(){
        if(sender.equals(0) || cost.equals(0) || super.isZero()){
            return true;
        }
        return false;
    }
    public void printMessage(){
        super.printMessage();
        System.out.println(", sender: " +Router.translateID(sender)+ 
                           ", cost: "+Integer.toString(cost)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(sender) 
                                + ", cost: "+Integer.toString(cost)+"}";
    }
}