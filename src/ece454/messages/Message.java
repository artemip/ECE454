package ece454.messages;

import ece454.MessageSender;
import ece454.PeerDefinition;
import ece454.PeersList;

import java.io.Serializable;

public class Message implements Serializable {
    protected int senderPort;
    protected PeerDefinition recipient;
    protected int senderId;

    protected Message() {
    }

    public Message(PeerDefinition recipient, int senderId, int senderPort) {
        this.recipient = recipient;
        this.senderId = senderId;
        this.senderPort = senderPort;
    }

    public PeerDefinition getRecipient() {
        return recipient;
    }

    public int getSenderId() {
        return this.senderId;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public static void broadcast(MessageSender sender, int senderId, int senderPort) {
        for (PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new Message(pd, senderId, senderPort));
        }
    }
}
