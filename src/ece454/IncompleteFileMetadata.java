package ece454;

import java.io.Serializable;

public class IncompleteFileMetadata implements Serializable {
    private String fileName;
    //Array of boolean values indicating whether we have the chunk or not
    private boolean[] chunkAvailability;
    private long dataSize;

    public IncompleteFileMetadata(boolean[] chunkAvailability, long dataSize, String fileName) {
        this.chunkAvailability = chunkAvailability;
        this.dataSize = dataSize;
        this.fileName = fileName;
    }

    public boolean[] getChunkAvailability() {
        return chunkAvailability;
    }

    public long getDataSize() {
        return dataSize;
    }

    public String getFileName() {
        return fileName;
    }
}
