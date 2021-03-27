package app;

import java.nio.ByteBuffer;

public class FIN extends Message{
    private Integer sender;
    private Integer receiver;

    public FIN(){
        sender = -1;
        receiver = -1;
    }

    public FIN(Integer t, Integer s, Integer r){
        super.setSize(20);
        super.setType(t);
        sender = s;
        receiver = r;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return sender;}
    public Integer getReceiver(){return receiver;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        sender = mess.getInt();
        receiver = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(20);
        super.writeMessage(b);
        b.putInt(sender);
        b.putInt(receiver);
        return b;
    }
    public Boolean isZero(){
        if(sender.equals(0) || receiver.equals(0) || super.isZero()){
            return true;
        }
        return false;
    }
    public void printMessage(){
        super.printMessage();
        System.out.println(", sender: " +Router.translateID(sender)+
                           ", receiver: " +Router.translateID(receiver)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(sender)
                                + ", receiver: " +Router.translateID(receiver)+"}";
    }
}