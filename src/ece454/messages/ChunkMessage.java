package ece454.messages;

import ece454.MessageSender;
import ece454.PeerDefinition;
import ece454.PeersList;
import ece454.storage.Chunk;

public class ChunkMessage extends Message {
    private Chunk chunk;

    public ChunkMessage(Chunk chunk, PeerDefinition recipient, int senderId, int senderPort) {
        super(recipient, senderId, senderPort);
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public static void broadcast(Chunk c, MessageSender sender, int senderId, int senderPort) {
        for (PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new ChunkMessage(c, pd, senderId, senderPort));
        }
    }
}
