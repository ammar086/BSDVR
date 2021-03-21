package app;

import java.nio.ByteBuffer;

public class HELLO extends Message{
    private Integer sender;
    public HELLO(){this.sender = -1;}
    public HELLO(Integer t, Integer s){
        super.setSize(16);
        super.setType(t);
        this.sender = s;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return this.sender;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        this.sender = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(16);
        super.writeMessage(b);
        b.putInt(this.sender);
        return b;
    }
    public Boolean isZero(){
        if(sender.equals(0) || super.isZero()){
            return true;
        }
        return false;
    }
    public void printMessage(){
        super.printMessage();
        System.out.println(", sender: " +Router.translateID(this.sender)+"}");
    }
}