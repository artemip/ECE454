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

    private String fileName;
    private Chunk[] chunks;
    private long size;
    private Set<Integer> incompleteChunks;
    private boolean isComplete;
    private int lastSync;

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
        this.isComplete = false;

        try {
            this.size = metadata.getDataSize();
            this.fileName = metadata.getFileName();

            File f = new File(metadata.getFileName());

            if(!f.exists())
                f.createNewFile();

            FileOutputStream fos = new FileOutputStream(metadata.getFileName());
            fos.write(INCOMPLETE_FILE_MAGIC_HEADER);

            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(metadata);
            oos.close();
            fos.close();

            RandomAccessFile raf = new RandomAccessFile(this.fileName, "rw");
            raf.setLength(raf.length() + metadata.getDataSize());
            raf.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Import an existing file into the P2P host (read existing data from file)
     * @param path fileName of the file to add
     * @throws FileNotFoundException
     */
    public DistributedFile(String path) throws FileNotFoundException {
        File file = new File(path);
        this.fileName = path;

        if(!file.exists()) {
            throw new FileNotFoundException("File at " + path + " does not exist.");
        }

        try {
            this.isComplete = isCompleteFile(path);
            int numChunks = (int)((file.length()) / Chunk.MAX_CHUNK_SIZE);
            ArrayList<Chunk> chunks = new ArrayList<Chunk>(numChunks);
            FileInputStream f = new FileInputStream(file);

            this.incompleteChunks = new HashSet<Integer>();
            byte[] readChunk = new byte[Chunk.MAX_CHUNK_SIZE];
            int numBytesRead;
            int index = 0;

            if(this.isComplete) {
                this.size = file.length();

                boolean[] chunkAvailability = new boolean[(int)Math.ceil(this.size / Chunk.MAX_CHUNK_SIZE)];
                for(int i = 0; i < chunkAvailability.length; ++i) {
                    chunkAvailability[i] = true;
                }

                IncompleteFileMetadata metadata = new IncompleteFileMetadata(chunkAvailability, this.size, this.fileName);

                // Read the entire file chunk-by-chunk
                while((numBytesRead = f.read(readChunk)) > -1) {
                    chunks.add(new Chunk(this.fileName, index++, numBytesRead, readChunk, metadata));
                }
            } else {
                // Read header information
                IncompleteFileMetadata metadata = getIncompleteFileMetadata(path);

                boolean[] chunkAvailability = metadata.getChunkAvailability();

                //Skip ahead in the file stream to get to the data
                readMagicHeader(f);
                readIncompleteFileMetadata(f);

                // Read file data, replacing empty data chunks with 'null's
                while((numBytesRead = f.read(readChunk)) > -1) {
                    if(chunkAvailability[index]) {
                        chunks.add(new Chunk(this.fileName, index, numBytesRead, readChunk, metadata));
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

            // Save every 20%
            int numChunks = this.chunks.length;
            int completeChunks = numChunks - this.incompleteChunks.size();

            if(completeChunks - lastSync > numChunks * 0.2) {
                lastSync = completeChunks;
                save();
            }
        }
    }

    public void save() {
        try {
            File f = new File(this.fileName);

            if(!f.exists())
                f.createNewFile();

            FileOutputStream fos = new FileOutputStream(this.fileName);

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

                IncompleteFileMetadata metadata = new IncompleteFileMetadata(chunkAvailability, this.size, this.fileName);
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
        return chunks[chunkId] != null;
    }

    public Chunk[] getChunks() {
        return chunks;
    }

    public String getFileName() {
        return this.fileName;
    }

    public boolean isComplete() {
        return this.isComplete;
    }
}
