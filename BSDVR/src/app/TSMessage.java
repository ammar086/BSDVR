package app;

import java.util.Date;
import java.time.Instant;
import java.text.SimpleDateFormat;


public class TSMessage {
    private long timestampA; // timestamp on arrival of packet
    private long timestampB; // timestamp on computing changes after arrival of packet
    private Message message;

    public TSMessage(Message m, long tsa, long tsb){
        message = m;
        timestampA = tsa;
        timestampB = tsb;
    }
    // getters
    public long getTimestampA(){
        return timestampA;
    }
    public long getTimestampB(){
        return timestampB;
    }
    public Message getMessage(){
        return message;
    }
    public String printTSMessage(){
        String s = message.toString();
        Date dt = Date.from(Instant.ofEpochMilli(timestampA));
        SimpleDateFormat formatter = new SimpleDateFormat("dd MM yyyy HH:mm:ss");
        s = s + " - " + formatter.format(dt);
        return s;
    }
}