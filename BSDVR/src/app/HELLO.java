package app;

import java.nio.ByteBuffer;

public class HELLO extends Message{
    private Integer sender;
    public HELLO(){sender = -1;}
    public HELLO(Integer t, Integer s){
        super.setSize(16);
        super.setType(t);
        sender = s;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return sender;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        sender = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(16);
        super.writeMessage(b);
        b.putInt(sender);
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
        System.out.println(", sender: " +Router.translateID(sender)+"}");
    }
}