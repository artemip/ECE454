package ece454p1;

import java.io.*;
import java.util.ArrayList;

public class DistributedFile {
    /**
     * Format of a temporary (incomplete) file:
     * [Magic Header]
     * [File Metadata] => [List of complete chunks][Data size]
     * [Chunk 0] (data-only; NOT a Chunk object)
     * [Chunk 1]
     * ...
     * [Chunk N]
     */

    //Uniquely identify an incomplete file from a complete one
    private byte[] INCOMPLETE_FILE_MAGIC_HEADER = new byte[] { 'T', 'E', 'M', 'P', 'F', 'I', 'L', 'E', 0 };

    private class IncompleteFileMetadata implements Serializable {
        //Array of boolean values indicating whether we have the chunk or not
        private boolean[] chunkAvailability;
        private int dataSize;

        public IncompleteFileMetadata(boolean[] chunkAvailability, int dataSize) {
            this.chunkAvailability = chunkAvailability;
            this.dataSize = dataSize;
        }

        private boolean[] getChunkAvailability() {
            return chunkAvailability;
        }

        private int getDataSize() {
            return dataSize;
        }
    }

    private String path;
    private Chunk[] chunks;

    private byte[] readMagicHeader(FileInputStream fis) throws IOException {
        byte[] magicHeaderArray = new byte[INCOMPLETE_FILE_MAGIC_HEADER.length];
        fis.read(magicHeaderArray);
        return magicHeaderArray;
    }

    private IncompleteFileMetadata readIncompleteFileMetadata(FileInputStream fis) throws IOException {
        IncompleteFileMetadata metadata = null;
        ObjectInputStream metadataInputStream = new ObjectInputStream(fis);
        try {
            Object o = metadataInputStream.readObject();
            if(o instanceof IncompleteFileMetadata)
                metadata = (IncompleteFileMetadata)o;
            else
                throw new ClassNotFoundException("Could not read metadata object from file");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return metadata;
    }

    private boolean isCompleteFile(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        byte[] magicHeaderArray = readMagicHeader(fis);
        fis.close();

        for(int i = 0; i < magicHeaderArray.length; ++i) {
            if(magicHeaderArray[i] != INCOMPLETE_FILE_MAGIC_HEADER[i])
                return false;
        }

        return true;
    }

    private IncompleteFileMetadata getIncompleteFileMetadata(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);

        //Skip the magic header
        readMagicHeader(fis);

        IncompleteFileMetadata metadata = readIncompleteFileMetadata(fis);

        fis.close();

        return metadata;
    }

    public DistributedFile(String path) throws FileNotFoundException {
        //TODO: What if the file is not complete?

        this.path = path;
        File file = new File(path);
        String relativePath = Config.FILES_DIRECTORY + "/" + file.getName();

        if(!file.exists()) {
            throw new FileNotFoundException("File at " + path + " does not exist.");
        }

        try {
            boolean isCompleteFile = isCompleteFile(path);
            int numChunks = (int)((file.length()) / Chunk.MAX_CHUNK_SIZE);
            ArrayList<Chunk> chunks = new ArrayList<Chunk>(numChunks);
            FileInputStream f = new FileInputStream(file);

            if(!isCompleteFile) {
                IncompleteFileMetadata metadata = getIncompleteFileMetadata(path);
                numChunks = (int)(metadata.getDataSize() / Chunk.MAX_CHUNK_SIZE);
            } else {

            }

            byte[] readChunk = new byte[Chunk.MAX_CHUNK_SIZE];
            int numBytesRead;
            int index = 0;

            while((numBytesRead = f.read(readChunk)) > -1) {
                chunks.add(new Chunk(relativePath, index++, numBytesRead, readChunk));
            }

            this.chunks = chunks.toArray(this.chunks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Chunk[] getChunks() {
        return chunks;
    }
}
