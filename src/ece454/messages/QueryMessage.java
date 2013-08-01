package ece454.messages;

import ece454.MessageSender;
import ece454.PeerDefinition;
import ece454.PeersList;

public class QueryMessage extends Message {

    public QueryMessage(PeerDefinition recipient, int senderId, int senderPort) {
        super(recipient, senderId, senderPort);
    }

    public static void broadcast(MessageSender msgSender, int senderId, int senderPort) {
        for (PeerDefinition pd : PeersList.getPeers()) {
            msgSender.sendMessage(new QueryMessage(pd, senderId, senderPort));
        }
    }
}

