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
    public Client(Socket server, Router router, Integer cost){
        this.server = server;
        this.router = router;
        this.cost = cost;
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
                    m = new SYN(1,this.router.getID(),this.cost);
                    this.router.send(out, m); // initiate handshake
                }
                m = this.router.receive(in); // receive incoming messages from one client
                if(!m.isZero()){    
                    type = m.getType();
                    if(type == 2){SYN_ACK sa = (SYN_ACK) m; ne = Router.translateID(sa.getSender());}
                    flag = false;
                    // routing protocol
                    new Protocols().BSDVRP(this.router,m,in,out,this.server);
                }
            }
        }catch(Exception e){
            this.router.printException(e,Router.translateID(router.getID()),ne);
            //System.out.printf("\n" + Router.translateID(this.router.getID()) + ":> ");
        }
    }
}