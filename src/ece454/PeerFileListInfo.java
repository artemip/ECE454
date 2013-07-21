package ece454;

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;

public class PeerFileListInfo implements Serializable{
    private Hashtable<String, boolean[]> peerFileHashTable;
    
    public PeerFileListInfo(Collection<DistributedFile> files){
    	this.peerFileHashTable = new Hashtable<String,boolean[]>();

        for(DistributedFile f : files) {    		
    		boolean[] chunkAvailability = new boolean[f.getChunks().length];
    		
    		for (int i=0; i<f.getChunks().length; i++){
    			chunkAvailability[i] = true;
    		}
    		
            for(Integer i : f.getIncompleteChunks()) {
                chunkAvailability[i] = false;
            }    		
            peerFileHashTable.put(f.getFileName(), chunkAvailability);
    	}
    }
    
    public Hashtable<String, boolean[]> getPeerFileHashTable(){
    	return this.peerFileHashTable;
    }
}
