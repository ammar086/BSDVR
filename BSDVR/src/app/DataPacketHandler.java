package app;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;


public class DataPacketHandler {
    private Router router;
    // on receiving DATA_PAYLOAD/DATA_SUMMARY messages
    public DataPacketHandler(Router r, Boolean flag, Message m){
        this.router = r;
        router.getDPHLock().lock();
        Boolean prompt_File_Received;
        Integer sender, next_hop, state;
        ArrayList<byte[]> message_digests, compared_digests;
        message_digests = new ArrayList<byte[]>();
        if(flag){
            // DATA PAYLOAD
            DATA_PAYLOAD dp;
            byte[] digest,content;
            String file_id, file_path;
            Integer total_chunks, dest;

            dp = (DATA_PAYLOAD)m;
            digest = dp.getDigest();
            dest = dp.getDestination();
            total_chunks = dp.getTotalChunks();
            file_id = new String(dp.getFile());
            // decrement num_hops time_to_live field of message
            dp.setHops(dp.getHops() - 1);
            content = dp.writeMessage().array();
            if(dest.equals(router.getID())){
                // payload destined to self
                // add to file
                router.addFileChunk(m);
                if(router.getFileBuff().get(file_id).size() == total_chunks){
                    prompt_File_Received = true;
                    if(prompt_File_Received){
                        System.out.println("\n\n"+Router.translateID(router.getID())+" FILE RECEIVED: "+ file_id+", "+router.getFileBuff().get(file_id).size()+" chunks\n");
                    }
                    // store received file locally
                    try {
                        file_path = router.path.replace("bin/", "");
                        storeFile(file_path, file_id);
                    } catch (Exception e) {
                        router.printException(e);
                    }
                }
            }else{
                // payload not destined to self
                // invoke drop policy if buffer full
                if(router.getBuffState().size() == Constants.BUFFER_CAPACITY){router.DropPolicy();}
                // check if space available in buffer for new packet
                if(Constants.BUFFER_CAPACITY > router.getBuffState().size()){
                    // add to buffer
                    router.addBuffPkt(digest, content, dest);
                    // call forwarding function if entry available for packet destination
                    if(router.getFT().containsKey(dest)){
                        message_digests.add(digest);
                        next_hop = router.getCurrentNextHopFT(dest);
                        state = router.getLT().get(next_hop).getLinkState();
                        if(state == 1){sendPayload(message_digests,next_hop);}
                    }
                }
            }
        }else{
            // DATA SUMMARY
            DATA_SUMMARY ds = (DATA_SUMMARY)m;
            sender = ds.getSender();
            if(ds.getMode() == 0){  
                // DATA_SUMMARY request
                // extract digests
                message_digests = extractDigests(m);
                // compare with digests in buffer
                compared_digests = compareDigests(message_digests);
                // generate and send summary response
                router.sendSummary(compared_digests,sender,1);
            }else{  
                // DATA_SUMMARY response
                // extract digests
                message_digests = extractDigests(m);
                // generate and send payload message
                if(router.getLT().containsKey(sender)){
                    state = router.getLT().get(sender).getLinkState();
                    if(state == 1){sendPayload(message_digests,sender);}
                }
            } 
        }
        router.getDPHLock().unlock();
    }
    // to initiate file transmission from source router
    public DataPacketHandler(Router r){
        this.router = r;
        router.getDPHLock().lock();
        Integer next_hop;
        ArrayList<byte[]> summary_vec;
        // send relevant summary vector for a file (already loaded in buffer) to a destination
        if(router.getBuffContent().size() > 0){
            for(Integer dest: router.getFT().keySet()){
                summary_vec = router.getSummary(dest);
                next_hop = router.getCurrentNextHopFT(dest);
                router.sendSummary(summary_vec,next_hop,0);
            }
        }
        router.getDPHLock().unlock();
    }
    // on encountering new neighbor
    public DataPacketHandler(Router r, Integer neighbor){
        this.router = r;
        router.getDPHLock().lock();
        // send relevant summary vector
        if(router.getBuffContent().size() > 0){
            ArrayList<byte[]> summary_vec;
            summary_vec = router.getSummary(neighbor);
            router.sendSummary(summary_vec,neighbor,0);
        }
        router.getDPHLock().unlock();
    }
    // on update in ft
    public DataPacketHandler(Router r, ArrayList<Integer> changes){
        this.router = r;
        router.getDPHLock().lock();
        Integer next_hop;
        ArrayList<byte[]> summary_vec;
        if(router.getBuffContent().size() > 0){
            for(Integer dest: changes){
                summary_vec = router.getSummary(dest);
                next_hop = router.getCurrentNextHopFT(dest);
                router.sendSummary(summary_vec,next_hop,0);
            }
        }
        router.getDPHLock().unlock();
    }
    // methods
    // extract digests from received DATA SUMMARY messages
    public ArrayList<byte[]> extractDigests(Message m){
        Integer inx, num_digests;
        DATA_SUMMARY ds = (DATA_SUMMARY)m;
        byte[] curr_digest, combined_digests;
        ArrayList<byte[]> message_digests = new ArrayList<byte[]>();
        inx = 0;
        num_digests = ds.getNum();
        combined_digests = ds.getSummary();
        curr_digest = new byte[Constants.DIGEST_SIZE];
        for(int i = 0 ; i < num_digests; i++){
            try {
                curr_digest = Arrays.copyOfRange(combined_digests, inx, inx+Constants.DIGEST_SIZE);
                message_digests.add(curr_digest);
                inx += Constants.DIGEST_SIZE;    
            } catch (Exception e) {
                router.printException(e);
                continue;
            }
        }
        return message_digests;
    }
    // compares digest in received DATA SUMMARY against those in buffer
    public ArrayList<byte[]> compareDigests(ArrayList<byte[]> message_digests){
        ArrayList<byte[]> compared_digests = new ArrayList<byte[]>();
        for(byte[] digest:message_digests){
            if(!router.getBuffState().containsKey(digest)){
                compared_digests.add(digest);
            }
        }
        return compared_digests;
    }
    // sends DATA PAYLOAD to next_hop of given destination(s)
    public void sendPayload(ArrayList<byte[]> message_digests, Integer next_hop){
        Message m;
        OutputStream out;
        Integer dest, state;
        DATA_PAYLOAD dp = new DATA_PAYLOAD();
        ConcurrentHashMap<byte[],byte[]> buffer_entry;
        byte[] curr_digest = new byte[Constants.DIGEST_SIZE];
        ByteBuffer curr_payload = ByteBuffer.allocate(Constants.MTU);
        Iterator<ConcurrentHashMap<byte[], byte[]>> inx = router.getBuffContent().iterator();
        while(inx.hasNext()){
            buffer_entry = inx.next();
            curr_digest = (byte[]) buffer_entry.keySet().toArray()[0];
            for(byte[] digest:message_digests){
                if(Arrays.equals(curr_digest, digest)){
                    try {
                        curr_payload = ByteBuffer.wrap(buffer_entry.get(curr_digest));
                        dp.readMessage(curr_payload);
                        dp.setSender(router.getID());
                        dp.setReceiver(next_hop);
                        m = dp;
                        dest = dp.getDestination();
                        state = router.getFT().get(dest).get(next_hop).getState();
                        out = router.getLT().get(next_hop).getWrite(next_hop);
                        router.send(out,m);
                        // System.out.println(state+1);
                        router.updateFlag(curr_digest,state+1);
                    } catch (Exception e) {
                        router.printExceptionWithNeighbor(e,Router.translateID(dp.getSender()),Router.translateID(dp.getReceiver()));
                    }
                }
            }
        }
    }
    // combines all chunks into original file
    public byte[] combineChunks(String file_id){
        ByteBuffer file_bytes;
        byte[] curr_chunk,file,payload;
        Integer chunk_size,total_chunks;
        DATA_PAYLOAD dp = new DATA_PAYLOAD();
        total_chunks = router.getFileBuff().get(file_id).size();
        payload = router.getFileBuff().get(file_id).get(1); // first chunk
        file_bytes = ByteBuffer.allocate(payload.length);
        file = new byte[0];
        for(int i = 0; i < total_chunks ; i++){
            if(i != 0){
                payload = router.getFileBuff().get(file_id).get(i+1);
            }
            file_bytes = ByteBuffer.wrap(payload);
            dp.readMessage(file_bytes);
            chunk_size = dp.getSize();
            curr_chunk = Arrays.copyOfRange(dp.getPayload(), 0, chunk_size);
            if(i == 0){
                file = curr_chunk;
            }else{
                file = router.combine(file, curr_chunk);
            }
        }
        return file;
    }
    // stores data file after complete transmission
    public void storeFile(String path, String file_id) throws IOException {
        File file;
        String fname;
        byte[] fdata;
        fname = file_id.substring(0, file_id.indexOf(file_id.charAt(file_id.length()-1)));
        file = new File(path+fname);
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(path+fname);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        fdata = combineChunks(file_id);
        fos.write(fdata);
        bos.flush();
        if (fos != null) fos.close();
        if (bos != null) bos.close();
        try {
            TimeUnit.SECONDS.sleep(3);
            router.getFileBuff().remove(file_id);
        } catch (Exception e) {
            router.printException(e);
        }
    }
}
