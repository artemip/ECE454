package ece454p1;

public class Chunk {
    public static final int MAX_CHUNK_SIZE = 65536;
    private String file;
    private int id;
    private int size;
    private byte[] data;

    public Chunk(String file, int id, int size, byte[] data) throws IllegalArgumentException {
        if (size > MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("Chunk size greater than MAX_CHUNK_SIZE: " + size + " > " + MAX_CHUNK_SIZE);
        }

        this.file = file;
        this.id = id;
        this.size = size;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public String getFile() {
        return file;
    }

    public int getSize() {
        return size;
    }
}
