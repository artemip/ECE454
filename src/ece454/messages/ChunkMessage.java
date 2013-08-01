package ece454.messages;

import ece454.MessageSender;
import ece454.PeerDefinition;
import ece454.PeersList;
import ece454.storage.Chunk;

public class ChunkMessage extends Message {
    private Chunk chunk;

    public ChunkMessage(Chunk chunk, PeerDefinition recipient, int senderId) {
        super(recipient, senderId);
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public static void broadcast(Chunk c, MessageSender sender, int senderId) {
        for (PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new ChunkMessage(c, pd, senderId));
        }
    }
}
