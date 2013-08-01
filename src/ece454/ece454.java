package ece454;

public class ece454 {
    public static void main(String[] args) {
        int peerId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);

        System.out.println("Starting server with id " + peerId + " on port " + port);

        if (peerId == 0) { //Start NAS
            NodeAddressServer nas = new NodeAddressServer();
            nas.join();
        } else { //Start peer
            Peer peer = new Peer(peerId, port);
            peer.join();
            peer.watchDirectory();
        }
    }
}
