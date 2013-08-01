package ece454.messages;


import ece454.MessageSender;
import ece454.PeerDefinition;
import ece454.PeersList;

public class NodeListMessage extends Message {
    private PeerDefinition[] nodeList;

    public NodeListMessage(PeerDefinition recipient, int senderId, int senderPort, PeerDefinition[] nodeList) {
        super(recipient, senderId, senderPort);
        this.nodeList = nodeList;
    }

    public PeerDefinition[] getNodeList() {
        return nodeList;
    }

    public static void broadcast(int senderPort, PeerDefinition[] nodeList, MessageSender sender, int senderId) {
        for (PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new NodeListMessage(pd, senderId, senderPort, nodeList));
        }
    }
}
