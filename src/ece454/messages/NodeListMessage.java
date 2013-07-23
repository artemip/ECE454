package ece454.messages;


import ece454.MessageSender;
import ece454.PeerDefinition;

public class NodeListMessage extends Message {

    private PeerDefinition[] nodeList;
    private int senderPort;

    public NodeListMessage(PeerDefinition recipient, int senderId, int senderPort, PeerDefinition[] nodeList) {
        super(recipient, senderId);
        this.nodeList = nodeList;
        this.senderPort = senderPort;
    }

    public PeerDefinition[] getNodeList() {
        return nodeList;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public static void broadcast(MessageSender sender, int senderId) { }
}
