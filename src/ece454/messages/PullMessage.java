package ece454.messages;

import ece454.PeerDefinition;
import ece454.MessageSender;
import ece454.PeersList;

public class PullMessage extends Message {
    public PullMessage(PeerDefinition recipient, int senderId) {
        super(recipient, senderId);
    }

    public static void broadcast(MessageSender sender, int senderId) {
        for(PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new PullMessage(pd, senderId));
        }
    }
}
