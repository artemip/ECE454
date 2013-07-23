package ece454;

import ece454.messages.NodeListMessage;
import ece454.util.ReturnCodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ece454 {
    public static void main(String[] args) {
        int peerId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);

        System.out.println("Starting server with id " + peerId + " on port " + port);

        if(peerId == 0) { //Start NAS
            NodeAddressServer nas = new NodeAddressServer();
            nas.join();
        } else { //Start peer
            Peer peer = new Peer(peerId, port);
            peer.join();
        }
    }
}
