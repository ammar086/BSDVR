package app;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Client implements Runnable {
    private Socket server;
    private Router router;
    private Integer cost;
    public Client(Socket s, Router r, Integer c){
        server = s;
        router = r;
        cost = c;
    }
    @Override
    public void run() {
        String ne = "";
        try{
            Message m;
            Integer type;
            OutputStream out = new DataOutputStream(server.getOutputStream());
            InputStream in = new DataInputStream(server.getInputStream());
            Boolean flag = true;
            while(!server.isClosed()){
                if(router.getServer().isClosed())break;
                if(flag){
                    m = new SYN(1,router.getID(),cost);
                    router.send(out, m); // initiate handshake
                }
                m = router.receive(in); // receive incoming messages from one client
                if(!m.isZero()){    
                    type = m.getType();
                    if(type == 2){SYN_ACK sa = (SYN_ACK) m; ne = Router.translateID(sa.getSender());}
                    flag = false;
                    // routing protocol
                    new Protocols().BSDVRP(router,m,in,out,server);
                }
            }
        }catch(Exception e){
            router.printExceptionWithNeighbor(e,Router.translateID(router.getID()),ne);
            //System.out.printf("\n" + Router.translateID(router.getID()) + ":> ");
        }
    }
}