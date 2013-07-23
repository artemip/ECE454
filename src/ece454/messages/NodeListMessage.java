package ece454.messages;


import ece454.MessageSender;
import ece454.PeerDefinition;

public class NodeListMessage extends Message {

    private PeerDefinition[] nodeList;

    public NodeListMessage(PeerDefinition recipient, int senderId, PeerDefinition[] nodeList) {
        super(recipient, senderId);
        this.nodeList = nodeList;
    }

    public PeerDefinition[] getNodeList() {
        return nodeList;
    }

    public static void broadcast(MessageSender sender, int senderId) { }
}
