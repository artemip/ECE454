package ece454;

import java.util.Hashtable;

/**
 * Status is the class that you populate with status data on the state of
 * replication in this peer and its knowledge of the replication level within
 * the system. The thing required in the Status object is the data as specified
 * in the private section The methods shown are examples of methods that we may
 * implement to access such data You will need to create methods to populate the
 * Status data.
 **/
public class Status {
	
	// This is very cheesy and very lazy, but the focus of this assignment 
	// is not on dynamic containers but on the BT p2p file distribution

	/* 
	 * The number of files currently in the system, 
	 * as viewed by this peer 
	 */
	private int numFiles;

	/*
	 * The fraction of the file present locally (= chunks on this peer/total
	 * number chunks in the file)
	 */
	private Hashtable<String, Float> local;

	/*
	 * The fraction of the file present in the system 
	 * (= chunks in the * system/total number chunks in the file) 
	 * (Note that if a chunk is present twice, 
	 * it doesn't get counted twice; this is simply intended to find out
	 * if we have the whole file in the system; 
	 * given that a file must be added at a peer, 
	 * think about why this number would ever not be 1.)
	 */
	private Hashtable<String, Float> system;

	/*
	 * Sum by chunk over all peers; the minimum of this number is the least
	 * replicated chunk, and thus represents the least level of 
	 * replication of  the file
	 */
	private Hashtable<String, Integer> leastReplication;

	/*
	 * Sum all chunks in all peers; 
	 * dived this by the number of chunks in the file; 
	 * this is the average level of replication of the file
	 */
	private Hashtable<String, Float> weightedLeastReplication;
	
	public Status(){
		this.numFiles = 0;
		this.local = new Hashtable<String, Float>();
		this.system = new Hashtable<String, Float>();
		this.leastReplication = new Hashtable<String, Integer>();
		this.weightedLeastReplication = new Hashtable<String, Float>();
}
	
	public int getNumFiles(){
		return numFiles;
	}

	public Hashtable<String, Float> getLocal(){
		return this.local;
	}
	
	public Hashtable<String, Float> getSystem(){
		return this.system;
	}
	
	public Hashtable<String, Integer> getLeastReplicated(){
		return this.leastReplication;
	}
	
	public Hashtable<String, Float> getWeightedLeastReplicated(){
		return this.weightedLeastReplication;
	}
		
	public void addLocalFile(String fileName, float fvalue){
		local.put(fileName, fvalue);
	}
	
	public void addSystemFile(String fileName, float fvalue){
		system.put(fileName, fvalue);
		numFiles++;
	}

	public void addLeastReplicatedChunk(String fileName, int value){
		leastReplication.put(fileName, value);
	}
	
	public void addWeightedLeastReplicated(String fileName, float fvalue){
		weightedLeastReplication.put(fileName, fvalue);
	}
}
