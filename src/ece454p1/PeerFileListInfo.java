package ece454p1;

import java.util.Collection;
import java.util.Hashtable;

public class PeerFileListInfo {
    //private int numFiles;
	//private List<String> fileNames;
    //private List<boolean[]> chunkAvailabilityList;
    private Hashtable<String, boolean[]> peerFileHashTable;
    
    public PeerFileListInfo(Collection<DistributedFile> files){
    	//this.numFiles = files.size();
        //this.fileNames = new ArrayList<String>();
        //this.chunkAvailabilityList = new ArrayList<boolean[]>();
        //this.numFiles = 0;
        
        for(DistributedFile f : files) {
    		//this.fileNames.add(f.getFileName());
    		
    		boolean[] chunkAvailability = new boolean[f.getChunks().length];
    		
    		for (int i=0; i<f.getChunks().length; i++){
    			chunkAvailability[i] = true;
    		}
    		
            for(Integer i : f.getIncompleteChunks()) {
                chunkAvailability[i] = false;
            }
    		
            //this.chunkAvailabilityList.add(chunkAvailability);
    		
            peerFileHashTable = new Hashtable<String,boolean[]>();
            peerFileHashTable.put(f.getFileName(), chunkAvailability);
            
            //this.numFiles++;
    	}
    }
    
//    public int getNumFiles(){
//    	return this.numFiles;
//    }
//    
//    public List<String> getFileNames(){
//    	return this.fileNames;
//    }
//    
//    public List<boolean[]> getChunkAvailabilityList(){
//    	return this.chunkAvailabilityList;
//    }
    
    public Hashtable<String, boolean[]> getPeerFileHashTable(){
    	return this.peerFileHashTable;
    }
}
