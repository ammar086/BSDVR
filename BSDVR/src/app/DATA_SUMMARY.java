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
        this.sender = -1;
        this.receiver = -1;
        this.mode = -1;
        this.num = -1;
        this.summary = null;
    }
    public DATA_SUMMARY(Integer t, Integer s, Integer r, Integer m, Integer n, byte[] su){
        super.setSize(28 + n * DIGEST_SIZE);
        super.setType(t);
        this.sender = s;
        this.receiver = r;
        this.mode = m;
        this.num = n;
        this.summary = su;
    }
   //getters
   public Integer getType(){return super.getType();}
   public Integer getSender(){return this.sender;}
   public Integer getReceiver(){return this.receiver;}
   public Integer getMode(){return this.mode;}
   public Integer getNum(){return this.num;}
   public byte[] getSummary(){return this.summary;}
   //methods
   public void readMessage(ByteBuffer mess){
       super.readMessage(mess);
       this.sender = mess.getInt();
       this.receiver = mess.getInt();
       this.mode = mess.getInt();
       this.num = mess.getInt();
       this.summary = new byte[this.num * DIGEST_SIZE];
       mess.get(summary);
   }
   public ByteBuffer writeMessage(){
       Integer s = 28 + num * DIGEST_SIZE;
       ByteBuffer b = ByteBuffer.allocate(s);
       super.writeMessage(b);
       b.putInt(this.sender);
       b.putInt(this.receiver);
       b.putInt(this.mode);
       b.putInt(this.num);
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
       System.out.println(", sender: " +Router.translateID(this.sender)+
                          ", receiver: " +Router.translateID(this.receiver)+
                          ", mode: "+Integer.toString(this.mode)+ 
                          ", num_vec: "+Integer.toString(this.num)+"}");
   }
   
   public String toString(){
        return super.toString() + ", sender: " +Router.translateID(this.sender)
                                + ", receiver: " +Router.translateID(this.receiver)
                                + ", mode: "+Integer.toString(this.mode)
                                + ", num_vec: "+Integer.toString(this.num)+"}";
   }
}