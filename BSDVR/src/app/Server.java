package app;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Server implements Runnable {
    private Socket client;
    private Router router;
    public Server(Socket client, Router router){
        this.client = client;
        this.router = router;
    }
    @Override
    public void run() {
        String ne = "";
        try{
            Message m;
            Integer type;
            OutputStream out = new DataOutputStream(client.getOutputStream());
            InputStream in = new DataInputStream(client.getInputStream());
            while(!client.isClosed()){
                if(router.getServer().isClosed())break;
                m = this.router.receive(in); // receive incoming messages from one client
                if(!m.isZero()){    
                    type = m.getType();
                    if(type == 1){SYN s = (SYN) m; ne = Router.translateID(s.getSender());}
                    //routing protocol
                    new Protocols().BSDVRP(this.router,m,in,out,this.client);
                }   
            }
        }catch(Exception e){
            this.router.printExceptionWithNeighbor(e, Router.translateID(router.getID()), ne);
        }
    }
}