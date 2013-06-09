package ece454p1;

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
        for(PeerDefinition pd : PeersList.getPeers()) {
            sender.sendMessage(new ChunkMessage(c, pd, senderId));
        }
    }
}
