package app;

import java.time.Instant;
import java.util.ArrayList;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class LinkFailureHandler implements Runnable{

    private Router router;
    private Integer neighbor;
    public LinkFailureHandler(Router r, Integer n){
        router = r;
        neighbor = n;
    }
    public Boolean isActive(){
        if(router.getLT().get(neighbor).getLinkState() == 1){
            // TODO: turned off for data plane emulation
            // long timer = router.getLT().get(neighbor).getTimer();
            // long timeDiff = Instant.now().toEpochMilli() - timer;
            // long timeElapsed = TimeUnit.MILLISECONDS.toSeconds(timeDiff);
            // if(timeElapsed < 30){return true;}
            return true;
        }
        return false;
    }
    @Override
    public void run() {
        Boolean flag = false;
        ArrayList<Integer> ch;
        long diff, period, timer;
        timer = Instant.now().toEpochMilli();
        while(isActive()){
            try{
                diff = Instant.now().toEpochMilli() - timer;
                period = TimeUnit.MILLISECONDS.toSeconds(diff);    
                // send HELLO pkts every 5 seconds
                if(period == 5){
                    timer = Instant.now().toEpochMilli();
                    Message m = new HELLO(6, router.getID());
                    OutputStream out = router.getLT().get(neighbor).getWrite(neighbor);
                    if(!(router.getServer().isClosed() && router.getLT().get(neighbor).getSocket().isClosed())){
                        router.send(out, m);
                    }
                }
            }catch(Exception e){
                router.printException(e);
                if(!flag)flag = true;
                timer = Instant.now().toEpochMilli();
                if(e.toString().contains("Broken pipe")){
                    System.out.println("Broken pipe in connection with " + neighbor.toString());
                }
            }
        }
        // on link failure detection
        try {
            Long tmp;
            Integer cost;
            Boolean cflag = false;                                               // for convergence and control traffic measurement
            tmp = Instant.now().toEpochMilli();                                  // convergence timer initialized
            if(router.getLT().get(neighbor).getLinkState() == 1){
                router.getLT().get(neighbor).setLinkState(0);
            }else{cflag = true;}
            if(!router.getLT().get(neighbor).getSocket().isClosed()){
                router.getLT().get(neighbor).getSocket().close();
            }
            // update entries in dvt
            cost = router.getLT().get(neighbor).getLinkCost();
            router.updateDVT(neighbor, neighbor, new Vector(cost, 0));           // fake path rule invoked for just neighbor
            // re-compute ft
            ch = router.computeFT();
            // measurement for convergence time and control traffic
            if(cflag){                                                           // counting FIN and FIN_ACK messages in TStat
                Message cm = router.getCMessage();
                router.getCstats().receiveMessageUpdate(cm.getType(),cm,ch,tmp);
                cflag = false;
            }
            // broadcast changes in ft
            new Protocols().broadcast(router,ch,neighbor,6);
        } catch (Exception e) {
            router.printException(e);
        }
    }
}
