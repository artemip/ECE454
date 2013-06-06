package ece454p1;

public class ece454p1 {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String peersFile = args[1];
        System.out.println("Starting server on port " + port + " using peers file at " + peersFile);

        PeersList.initialize(peersFile);
        MessageSender messageSender = new MessageSender();
        Peer peer = new Peer(port, messageSender);

        //TODO: Commandline interface
    }
}
