package ece454p1;

public class PullMessage extends Message {
    public PullMessage(PeerDefinition recipient) {
        super(recipient);
    }

    public static void broadcast(MessageSender sender) {
        for(PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new PullMessage(pd));
        }
    }
}
