package ece454.messages;

import ece454.MessageSender;
import ece454.PeerDefinition;
import ece454.PeersList;

public class DeleteMessage extends Message {

    private String filePath;

    public DeleteMessage(String filePath, PeerDefinition recipient, int senderId) {
        super(recipient, senderId);
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public static void broadcast(String path, MessageSender sender, int senderId) {
        for(PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new DeleteMessage(path, pd, senderId));
        }
    }
}
