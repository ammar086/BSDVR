package app;

import java.util.Arrays;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class Print {
    private Router router;

    public Print(Router r){
        router = r;
    }

    public void printLT() {
        String s = "";
        for(Integer n:router.getLT().keySet()){
            s = s + "{"+Router.translateID(n)+"="+router.getLT().get(n)+"}\n";
        }
        if(s != ""){
            s = s.substring(0, s.length() - 1);
            System.out.println("\nLT of " + Router.translateID(router.getID()) + ":\n" + s);
        }else{
            System.out.println("\nLT of " + Router.translateID(router.getID()) + ":\n" +  router.getLT());
        }
    }
    public void printFT() {
        String s = "";
        for (Integer m : router.getFT().keySet()) {
            s = s + "{dest " + Router.translateID(m) + "={";
            for (Integer n : router.getFT().get(m).keySet()) {
                s = s + "via " + Router.translateID(n) + "=" + router.getFT().get(m).get(n);
            }
            s = s + "}\n";
        }
        if(s != ""){
            s = s.substring(0, s.length() - 1);
            System.out.println("\nFT of " + Router.translateID(router.getID()) + ":\n" + s);
        }else{
            System.out.println("\nFT of " + Router.translateID(router.getID()) + ":\n" + router.getFT()); 
        }
        
    }
    public void printDVT() {
        String s = "";
        for (Integer m : router.getDVT().keySet()) {
            s = s + "{via " + Router.translateID(m) + "={";
            Integer temp = 0;
            for (Integer n : router.getDVT().get(m).keySet()) {
                if (temp != n && temp != 0) {
                    s = s + ", ";
                }
                s = s + "dest " + Router.translateID(n) + "=" + router.getDVT().get(m).get(n);
                temp = n;
            }
            s = s + "}\n";
        }
        if(s != ""){
            s = s.substring(0, s.length() - 1);
            System.out.println("\nDVT of " + Router.translateID(router.getID()) + ":\n" + s);
        }else{
            System.out.println("\nDVT of " + Router.translateID(router.getID()) + ":\n" + router.getDVT());
        }
    }
    public void printRouter(){
        System.out.println("\nRouter:\n" + "{id: " + Router.translateID(router.getID()) + ", port: "
                + Integer.toString(router.getServer().getLocalPort()) + ", neighbors: "
                + Integer.toString(router.getLT().keySet().size()) + "}");
    }
    public void printMBuffer(){
        byte[] di;
        String s = "";
        Integer dst, state;
        ConcurrentHashMap<byte[],byte[]> pkt;
        DATA_PAYLOAD dp = new DATA_PAYLOAD();
        ByteBuffer b = ByteBuffer.allocate(5000);
        Iterator<ConcurrentHashMap<byte[],byte[]>> iter = router.getBuffContent().iterator();
        while(iter.hasNext()){
            pkt = iter.next();
            di = (byte[]) pkt.keySet().toArray()[0];
            dst = (Integer) router.getBuffState().get(di).keySet().toArray()[0];
            state = router.getBuffState().get(di).get(dst);
            b = ByteBuffer.wrap(pkt.get(di));
            dp.readMessage(b);
            s = s + "\n{state: "+state+", pkt: "+dp.toString()+"}";
        }
        System.out.println("\nFRONT");
        System.out.println(s);
        System.out.println("bs: "+router.getBuffState().size()+" bc: "+router.getBuffContent().size());
        System.out.println("\nTAIL");
    }
    public void printDigests(){
        byte[] di;
        ConcurrentHashMap<byte[],byte[]> pkt;
        Iterator<ConcurrentHashMap<byte[],byte[]>> iter = router.getBuffContent().iterator();
        while(iter.hasNext()){
            pkt = iter.next();
            di = (byte[]) pkt.keySet().toArray()[0];
            System.out.println("\n"+Arrays.toString(di)+ " " + di.length);
        }
    }
    public void printFBuffer(){
        System.out.println("\nUnder Construction !");
    } 
}
