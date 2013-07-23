package ece454.messages;

import ece454.PeerDefinition;
import ece454.MessageSender;
import ece454.PeersList;

import java.io.Serializable;

public class Message implements Serializable {
    protected PeerDefinition recipient;
    protected int senderId;

    protected Message() { }

    public Message(PeerDefinition recipient, int senderId) {
        this.recipient = recipient;
        this.senderId = senderId;
    }

    public PeerDefinition getRecipient() {
        return recipient;
    }

    public int getSenderId() {
        return this.senderId;
    }

    public void setRecipient(PeerDefinition recipient) {
        this.recipient = recipient;
    }

    public static void broadcast(MessageSender sender, int senderId) {
        for(PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new Message(pd, senderId));
        }
    }
}
