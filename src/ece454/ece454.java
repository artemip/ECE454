package ece454;

import ece454.util.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ece454 {
    public static void main(String[] args) {
        int peerId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);

        System.out.println("Starting server with id " + peerId + " on port " + port);

        if (peerId == 0) { //Start NAS
            NodeAddressServer nas = new NodeAddressServer();
            nas.join();
        } else { //Start peer
            Config.FILES_DIRECTORY_NAME = args[2];
            Config.FILES_DIRECTORY = new File(Config.USER_HOME_DIRECTORY, Config.FILES_DIRECTORY_NAME);

            Peer peer = new Peer(peerId, port);
            peer.join();
            peer.watchDirectory();
            
            boolean leave = false;
            String userInput = "";
            
            while (!leave){
    	        System.out.println("Leave? (y/n)");

    	    	try{
    	    	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
    	    	    userInput = bufferRead.readLine();
    	    	}
    	    	catch(IOException e){
    	    		e.printStackTrace();
    	    	}
    	    	
    	    	if (userInput.equals("y")){
    	    		System.out.println("Peer " + peer.getId() + " is leaving ...");
    	    		leave = true;
    	    		peer.leave();
    	    	}
            }
            
        }
    }
}
