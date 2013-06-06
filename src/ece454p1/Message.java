package ece454p1;

public class Message {
    protected PeerDefinition recipient;

    public Message() { }

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
