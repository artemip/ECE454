package ece454p1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ece454p1 {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String peersFile = args[1];
        System.out.println("Starting server on port " + port + " using peers file at " + peersFile);

        PeersList.initialize(peersFile);
        MessageSender messageSender = new MessageSender();
        Peer peer = new Peer(port, messageSender);

        boolean  loop = true;
        String userInput = "";
        
        while(loop){
	        System.out.println("1. Insert");
	        System.out.println("2. Query");
	        System.out.println("3. Join");
	        System.out.println("4. Leave");
	        System.out.println("5. Exit");
	        
	    	try{
	    	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
	    	    userInput = bufferRead.readLine();
	    	}
	    	catch(IOException e){
	    		e.printStackTrace();
	    	}
	    	
	    	//Insert
	    	if (userInput.equals("1")){
	    		String fname = "";
	    		System.out.println("Enter file name:");
	    		try{
		    	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		    	    fname = bufferRead.readLine();
		    	}
		    	catch(IOException e){
		    		e.printStackTrace();
		    	}
	    		peer.insert(fname);
	    		
	    	}
	    	//Query
	    	else if (userInput.equals("2")){
	    		peer.query(new Status());
	    		
	    	}
	    	//Join
	    	else if (userInput.equals("3")){
	    		peer.join();
	    	}
	    	//Leave
	    	else if (userInput.equals("4")){
	    		peer.leave();
	    	}
	    	else if (userInput.equals("5")){
	    		loop = false;
	    	}
	    	else {
	    		System.out.println("Invalid input");
	    	}
	    }
    }
}
