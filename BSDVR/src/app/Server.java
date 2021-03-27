package app;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Server implements Runnable {
    private Socket client;
    private Router router;
    public Server(Socket c, Router r){
        client = c;
        router = r;
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
                m = router.receive(in); // receive incoming messages from one client
                if(!m.isZero()){    
                    type = m.getType();
                    if(type == 1){SYN s = (SYN) m; ne = Router.translateID(s.getSender());}
                    //routing protocol
                    new Protocols().BSDVRP(router,m,in,out,client);
                }   
            }
        }catch(Exception e){
            router.printExceptionWithNeighbor(e, Router.translateID(router.getID()), ne);
        }
    }
}