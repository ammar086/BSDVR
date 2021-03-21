package app;

import java.nio.ByteBuffer;

public class SYN extends Message{
    private Integer sender;
    private Integer cost;

    public SYN(){
        this.sender = -1;
        this.cost = -1;
    }
    
    public SYN(Integer t, Integer s, Integer c){
        super.setSize(20);
        super.setType(t);
        this.sender = s;
        this.cost = c;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return this.sender;}
    public Integer getCost(){return this.cost;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        this.sender = mess.getInt();
        this.cost = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(20);
        super.writeMessage(b);
        b.putInt(this.sender);
        b.putInt(this.cost);
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
        System.out.println(", sender: " +Router.translateID(this.sender)+ 
                           ", cost: "+Integer.toString(this.cost)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(this.sender) 
                                + ", cost: "+Integer.toString(this.cost)+"}";
    }
}