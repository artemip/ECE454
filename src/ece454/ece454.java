package ece454;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ece454 {
    public static void main(String[] args) {
        int peerId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        String peersFile = args[2];

        System.out.println("Starting server with id " + peerId + " on port " + port + " using peers file at " + peersFile);

        PeersList.initialize(peersFile, peerId);
        MessageSender messageSender = new MessageSender();
        Peer peer = new Peer(peerId, port, messageSender);

        boolean  loop = true;
        String userInput = "";
        
        while(loop){
	        System.out.println("1. Join");
	        System.out.println("2. Insert");
	        System.out.println("3. Query");
	        System.out.println("4. Leave");
	        System.out.println("5. Exit");
	        
	    	try{
	    	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
	    	    userInput = bufferRead.readLine();
	    	}
	    	catch(IOException e){
	    		e.printStackTrace();
	    	}
	    	
	    	//Join
	    	if (userInput.equals("1")){
	    		peer.join();
	    	}
	    	//Insert
	    	else if (userInput.equals("2")){
	    		String fname = "";
	    		System.out.println("Enter file name:");
	    		try{
		    	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		    	    fname = bufferRead.readLine();
		    	}
		    	catch(IOException e){
		    		e.printStackTrace();
		    	}

                int retCode = peer.insert(fname);
                switch(retCode) {
                    case ReturnCodes.FILE_NOT_FOUND:
                        System.err.println("File " + fname + " not found.");
                        break;
                    case ReturnCodes.FILE_COPY_ERR:
                        System.err.println("Could not copy file " + fname + " over to local directory.");
                        break;
                    default:
                }
	    	}
	    	//Query
	    	else if (userInput.equals("3")){
	    		Status s = new Status();
	    		
	    		peer.query(s);
	    		
	    		//print out parameters of Status object
	    		System.out.printf("Number of files in the System: %d \n", s.getNumFiles());
	    		System.out.println("Status.Local");
	    		System.out.println(s.getLocal().toString());
	    		System.out.println("Status.System");
	    		System.out.println(s.getSystem().toString());
	    		System.out.println("Status.LeastReplicated");
	    		System.out.println(s.getLeastReplicated().toString());
	    		System.out.println("Status.WeightedLeastReplicated");
	    		System.out.println(s.getWeightedLeastReplicated().toString());
	    		System.out.println();

	    	}
	    	//Leave
	    	else if (userInput.equals("4")){
	    		peer.leave();
	    	}
	    	else if (userInput.equals("5")){
                peer.leave();
                messageSender.shutdown();
	    		loop = false;
	    	}
	    	else {
	    		System.out.println("Invalid input");
	    	}
	    }

        System.exit(0);
    }
}
