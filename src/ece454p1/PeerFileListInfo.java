package ece454p1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PeerFileListInfo {
    private int numFiles;
	private List<String> fileNames;
    private List<boolean[]> chunkAvailabilityList;
    
    //TODO
    //Use this instead of chunkAvailabilityList
    //private List<Set<Integer>> incompleteChunks;
    //private int[] totalNumChunks;
    
    private int numTotalChunks;
    private int numAvailableChunks;
    
    public PeerFileListInfo(Collection<DistributedFile> files){
    	this.numFiles = files.size();
        this.fileNames = new ArrayList<String>();
        this.chunkAvailabilityList = new ArrayList<boolean[]>();
                
        for(DistributedFile f : files) {
    		this.fileNames.add(f.getFileName());
    		
    		boolean[] chunkAvailability = new boolean[f.getChunks().length];
    		
            for(Integer i : f.getIsComplete()) {
                chunkAvailability[i] = false;
            }
    		this.chunkAvailabilityList.add(chunkAvailability);
    	}
    }
    
    public int getNumFiles(){
    	return this.numFiles;
    }
    
    public List<String> getFileNames(){
    	return this.fileNames;
    }
    
    public List<boolean[]> getChunkAvailability(){
    	return this.chunkAvailabilityList;
    }
    
}
