package ece454p1;

import java.io.Serializable;

public class Message implements Serializable {
    protected PeerDefinition recipient;

    protected Message() { }

    public Message(PeerDefinition recipient) {
        this.recipient = recipient;
    }

    public PeerDefinition getRecipient() {
        return recipient;
    }

    public void setRecipient(PeerDefinition recipient) {
        this.recipient = recipient;
    }

    public static void broadcast(MessageSender sender) {
        for(PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new Message(pd));
        }
    }
}
