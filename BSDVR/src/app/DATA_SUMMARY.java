package app;

import java.nio.ByteBuffer;

public class DATA_SUMMARY extends Message {
    public static Integer DIGEST_SIZE = 20;
    private Integer sender;
    private Integer receiver;
    private Integer mode;
    private Integer num;
    private byte[] summary;
    public DATA_SUMMARY(){
        sender = -1;
        receiver = -1;
        mode = -1;
        num = -1;
        summary = null;
    }
    public DATA_SUMMARY(Integer t, Integer s, Integer r, Integer m, Integer n, byte[] su){
        super.setSize(28 + n * DIGEST_SIZE);
        super.setType(t);
        sender = s;
        receiver = r;
        mode = m;
        num = n;
        summary = su;
    }
   //getters
   public Integer getType(){return super.getType();}
   public Integer getSender(){return sender;}
   public Integer getReceiver(){return receiver;}
   public Integer getMode(){return mode;}
   public Integer getNum(){return num;}
   public byte[] getSummary(){return summary;}
   //methods
   public void readMessage(ByteBuffer mess){
       super.readMessage(mess);
       sender = mess.getInt();
       receiver = mess.getInt();
       mode = mess.getInt();
       num = mess.getInt();
       summary = new byte[num * DIGEST_SIZE];
       mess.get(summary);
   }
   public ByteBuffer writeMessage(){
       Integer s = 28 + num * DIGEST_SIZE;
       ByteBuffer b = ByteBuffer.allocate(s);
       super.writeMessage(b);
       b.putInt(sender);
       b.putInt(receiver);
       b.putInt(mode);
       b.putInt(num);
       b.put(summary);
       return b;
   }
   public Boolean isZero(){
       if(sender.equals(0) || receiver.equals(0) || mode.equals(-1) || num.equals(-1) || super.isZero()){
           return true;
       }
       return false;
   }
   public void printMessage(){
       super.printMessage();
       System.out.println(", sender: " +Router.translateID(sender)+
                          ", receiver: " +Router.translateID(receiver)+
                          ", mode: "+Integer.toString(mode)+ 
                          ", num_vec: "+Integer.toString(num)+"}");
   }
   
   public String toString(){
        return super.toString() + ", sender: " +Router.translateID(sender)
                                + ", receiver: " +Router.translateID(receiver)
                                + ", mode: "+Integer.toString(mode)
                                + ", num_vec: "+Integer.toString(num)+"}";
   }
}