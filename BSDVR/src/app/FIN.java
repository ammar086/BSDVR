package app;

import java.nio.ByteBuffer;

public class FIN extends Message{
    private Integer sender;
    private Integer receiver;

    public FIN(){
        this.sender = -1;
        this.receiver = -1;
    }

    public FIN(Integer t, Integer s, Integer r){
        super.setSize(20);
        super.setType(t);
        this.sender = s;
        this.receiver = r;
    }
    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return this.sender;}
    public Integer getReceiver(){return this.receiver;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        this.sender = mess.getInt();
        this.receiver = mess.getInt();
    }
    public ByteBuffer writeMessage(){
        ByteBuffer b = ByteBuffer.allocate(20);
        super.writeMessage(b);
        b.putInt(this.sender);
        b.putInt(this.receiver);
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
        System.out.println(", sender: " +Router.translateID(this.sender)+
                           ", receiver: " +Router.translateID(this.receiver)+"}");
    }
    public String toString(){
        return super.toString() + ", sender: " +Router.translateID(this.sender)
                                + ", receiver: " +Router.translateID(this.receiver)+"}";
    }
}