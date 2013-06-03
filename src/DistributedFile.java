package ece454p1;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    private String path;
    private Chunk[] chunks;
    private long size;
    private Set<Integer> incompleteChunks;

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

    /**
     * Create a new file to store external chunk data (i.e. for files not stored on this host)
     * @param metadata the metadata object for the external file
     */
    public DistributedFile(IncompleteFileMetadata metadata) throws IOException {
        try {
            this.size = metadata.getDataSize();
            this.path = metadata.getFileName();

            File f = new File(metadata.getFileName());

            if(!f.exists())
                f.createNewFile();

            FileOutputStream fos = new FileOutputStream(metadata.getFileName());
            fos.write(INCOMPLETE_FILE_MAGIC_HEADER);

            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(metadata);
            oos.close();
            fos.close();

            RandomAccessFile raf = new RandomAccessFile(this.path, "rw");
            raf.setLength(raf.length() + metadata.getDataSize());
            raf.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Import an existing file into the P2P host (read existing data from file)
     * @param path path of the file to add
     * @throws FileNotFoundException
     */
    public DistributedFile(String path) throws FileNotFoundException {
        File file = new File(path);
        this.path = path;

        if(!file.exists()) {
            throw new FileNotFoundException("File at " + path + " does not exist.");
        }

        try {
            boolean isCompleteFile = isCompleteFile(path);
            int numChunks = (int)((file.length()) / Chunk.MAX_CHUNK_SIZE);
            ArrayList<Chunk> chunks = new ArrayList<Chunk>(numChunks);
            FileInputStream f = new FileInputStream(file);

            this.incompleteChunks = new HashSet<Integer>();
            byte[] readChunk = new byte[Chunk.MAX_CHUNK_SIZE];
            int numBytesRead;
            int index = 0;

            if(isCompleteFile) {
                this.size = file.length();

                // Read the entire file chunk-by-chunk
                while((numBytesRead = f.read(readChunk)) > -1) {
                    chunks.add(new Chunk(this.path, index++, numBytesRead, readChunk));
                }
            } else {
                // Read header information
                IncompleteFileMetadata metadata = getIncompleteFileMetadata(path);

                boolean[] chunkAvailability = metadata.chunkAvailability;

                //Skip ahead in the filestream to get to the data
                readMagicHeader(f);
                readIncompleteFileMetadata(f);

                // Read file data, replacing empty data chunks with 'null's
                while((numBytesRead = f.read(readChunk)) > -1) {
                    if(chunkAvailability[index]) {
                        chunks.add(new Chunk(this.path, index, numBytesRead, readChunk));
                    } else {
                        chunks.add(null);
                        this.incompleteChunks.add(index);
                    }

                    index++;
                }
            }

            this.chunks = chunks.toArray(this.chunks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addChunk(Chunk chunk) {
        if(this.chunks[chunk.getId()] != null) {
            this.chunks[chunk.getId()] = chunk;
            this.incompleteChunks.remove(chunk.getId());
        }
    }

    public void save() {
        try {
            File f = new File(this.path);

            if(!f.exists())
                f.createNewFile();

            FileOutputStream fos = new FileOutputStream(this.path);

            boolean complete = this.incompleteChunks.isEmpty();

            if(complete) {
                for(Chunk c : this.chunks) {
                    fos.write(c.getData());
                }
            } else {
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                fos.write(INCOMPLETE_FILE_MAGIC_HEADER);

                boolean[] chunkAvailability = new boolean[this.chunks.length];

                for(Integer i : this.incompleteChunks) {
                    chunkAvailability[i] = false;
                }

                IncompleteFileMetadata metadata = new IncompleteFileMetadata(chunkAvailability, this.size, this.path);
                oos.writeObject(metadata);

                for(Chunk c : this.chunks) {
                    if(c == null) {
                        fos.write(new byte[Chunk.MAX_CHUNK_SIZE]);
                    } else {
                        fos.write(c.getData());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasChunk(int chunkId) {
        return chunks[chunkId] == null;
    }

    public Chunk[] getChunks() {
        return chunks;
    }
}
