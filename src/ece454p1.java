package ece454p1;

import java.io.IOException;

public class ece454p1 {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String peersFile = args[1];
        System.out.println("Starting server on port " + port + " using peers file at " + peersFile);

        PeersList.initialize(peersFile);
        Peer peer = new Peer(port);
        try {
            peer.startServerSocket();
        } catch (IOException e) {
            System.err.println("Could not open socket connection on port " + port + ": " + e.toString());
        }

        //TODO: Commandline interface
    }
}
