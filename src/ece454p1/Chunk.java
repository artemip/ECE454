package ece454p1;

import java.io.Serializable;

public class Chunk implements Serializable {
    public static final int MAX_CHUNK_SIZE = 65536;
    private int id;
    private byte[] data;
    private IncompleteFileMetadata metadata;

    public Chunk(int id, int size, byte[] data, IncompleteFileMetadata metadata) throws IllegalArgumentException {
        if (data.length > MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("Chunk size greater than MAX_CHUNK_SIZE: " + size + " > " + MAX_CHUNK_SIZE);
        }

        this.id = id;
        this.data = data;
        this.metadata = metadata;
    }

    public int getId() {
        return this.id;
    }

    public byte[] getData() {
        return this.data;
    }

    public IncompleteFileMetadata getMetadata() {
        return this.metadata;
    }
}
