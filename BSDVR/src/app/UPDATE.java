package app;

import java.nio.ByteBuffer;

public class UPDATE extends Message{
    private Integer sender;
    private Integer receiver;
    private Integer num_updates;
    private byte[] dvt_updates;

    public UPDATE(){
        sender = -1;
        receiver = -1;
        num_updates = -1;
        dvt_updates = null;
    }

    public UPDATE(Integer t, Integer s, Integer r, Integer n, byte[] udvt){
        super.setSize(24 + n * 12);
        super.setType(t);
        sender = s;
        receiver = r;
        num_updates = n;
        dvt_updates = udvt;
    }
    // single update constructor
    public UPDATE(Integer t, Integer s, Integer r, Integer n, Integer dest, Integer cost, Integer state){
        super.setSize(24 + n * 12);
        super.setType(t);
        sender = s;
        receiver = r;
        num_updates = n;
        ByteBuffer udvt = ByteBuffer.allocate(12);
        udvt.putInt(dest);
        udvt.putInt(cost);
        udvt.putInt(state);
        dvt_updates = udvt.array();
    }

    //getters
    public Integer getType(){return super.getType();}
    public Integer getSender(){return sender;}
    public Integer getReceiver(){return receiver;}
    public Integer getNUpdates(){return num_updates;}
    public byte[] getUpdates(){return dvt_updates;}
    //methods
    public void readMessage(ByteBuffer mess){
        super.readMessage(mess);
        sender = mess.getInt();
        receiver = mess.getInt();
        num_updates = mess.getInt();
        dvt_updates = new byte[num_updates * 12];
        mess.get(dvt_updates);
    }
    public ByteBuffer writeMessage(){
        Integer s = 24 + num_updates * 12;
        ByteBuffer b = ByteBuffer.allocate(s);
        super.writeMessage(b);
        b.putInt(sender);
        b.putInt(receiver);
        b.putInt(num_updates);
        b.put(dvt_updates);
        return b;
    }
    public Boolean isZero(){
        if(sender.equals(0) || receiver.equals(0) || num_updates.equals(0) || super.isZero()){
            return true;
        }
        return false;
    }
    public void printMessage(){
        super.printMessage();
        String udvt = "{";
        Vector nvec;
        Integer dest, cost, state;
        ByteBuffer updates = ByteBuffer.wrap(dvt_updates);
        for (int i = 0; i < num_updates; i++) {
            dest = updates.getInt();
            cost = updates.getInt();
            state = updates.getInt();
            nvec = new Vector(cost, state);
            udvt += "{d: "+Router.translateID(dest)+" "+"v: "+nvec.toString()+"}";
            if(i<num_updates-1){
                udvt+= ", ";
            }
        }
        udvt+="}";
        System.out.println(", sender: " +Router.translateID(sender)+
                           ", receiver: " +Router.translateID(receiver)+ 
                           ", num_updates: "+Integer.toString(num_updates)+
                           ", dvt_updates: "+udvt+"}");
    }
    public String toString(){
        String udvt = "{";
        Vector nvec;
        Integer dest, cost, state;
        ByteBuffer updates = ByteBuffer.wrap(dvt_updates);
        for (int i = 0; i < num_updates; i++) {
            dest = updates.getInt();
            cost = updates.getInt();
            state = updates.getInt();
            nvec = new Vector(cost, state);
            udvt += "{d: "+Router.translateID(dest)+" "+"v: "+nvec.toString()+"}";
            if(i<num_updates-1){
                udvt+= ", ";
            }
        }
        udvt+="}";
        return super.toString() + ", sender: " +Router.translateID(sender)
                                + ", receiver: " +Router.translateID(receiver)
                                + ", num_updates: "+Integer.toString(num_updates)
                                + ", dvt_updates: "+udvt+"}";
    }
}
