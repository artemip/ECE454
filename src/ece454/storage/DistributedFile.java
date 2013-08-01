package ece454.storage;

import ece454.util.Config;

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
    private byte[] INCOMPLETE_FILE_MAGIC_HEADER = new byte[]{'T', 'E', 'M', 'P', 'F', 'I', 'L', 'E', 0};

    private String fileName;
    private Chunk[] chunks = new Chunk[0];
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
            if (o instanceof IncompleteFileMetadata)
                metadata = (IncompleteFileMetadata) o;
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

        for (int i = 0; i < magicHeaderArray.length; ++i) {
            if (magicHeaderArray[i] != INCOMPLETE_FILE_MAGIC_HEADER[i])
                return true;
        }

        return false;
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
     *
     * @param metadata the metadata object for the external file
     */
    public DistributedFile(IncompleteFileMetadata metadata) throws IOException {
        this.isComplete = false;

        try {
            this.size = metadata.getDataSize();
            this.fileName = metadata.getFileName();

            File f = new File(Config.FILES_DIRECTORY + "/" + metadata.getFileName());

            if (!f.exists()) {
                File parent = f.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Couldn't create dir: " + parent);
                }

                f.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(INCOMPLETE_FILE_MAGIC_HEADER);

            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(metadata);
            oos.close();
            fos.close();

            RandomAccessFile raf = new RandomAccessFile(Config.FILES_DIRECTORY + "/" + this.fileName, "rw");
            raf.setLength(raf.length() + metadata.getDataSize());
            raf.close();

            this.chunks = new Chunk[metadata.getChunkAvailability().length];

            this.incompleteChunks = new HashSet<Integer>();
            for (int i = 0; i < this.chunks.length; ++i) {
                this.incompleteChunks.add(i);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Import an existing file into the P2P host (read existing data from file)
     *
     * @param path fileName of the file to add
     * @throws FileNotFoundException
     */
    public DistributedFile(String path) throws FileNotFoundException {
        this.fileName = path;

        File file = new File(Config.FILES_DIRECTORY + "/" + path);

        if (!file.exists()) {
            throw new FileNotFoundException("File at " + path + " does not exist.");
        }

        try {
            this.isComplete = isCompleteFile(Config.FILES_DIRECTORY + "/" + path);
            int numChunks = (int) Math.ceil(((double) file.length()) / Chunk.MAX_CHUNK_SIZE);
            ArrayList<Chunk> chunks = new ArrayList<Chunk>(numChunks);
            FileInputStream fis = new FileInputStream(file);

            this.incompleteChunks = new HashSet<Integer>();
            byte[] readChunk = new byte[Chunk.MAX_CHUNK_SIZE];
            int numBytesRead;
            int index = 0;

            if (this.isComplete) {
                this.size = file.length();

                boolean[] chunkAvailability = new boolean[numChunks];
                for (int i = 0; i < chunkAvailability.length; ++i) {
                    chunkAvailability[i] = true;
                }

                IncompleteFileMetadata metadata = new IncompleteFileMetadata(chunkAvailability, this.size, this.fileName);

                // Read the entire file chunk-by-chunk
                while ((numBytesRead = fis.read(readChunk)) > -1) {
                    chunks.add(new Chunk(index++, numBytesRead, readChunk, metadata));
                }
            } else {
                // Read header information
                IncompleteFileMetadata metadata = getIncompleteFileMetadata(path);

                boolean[] chunkAvailability = metadata.getChunkAvailability();

                //Skip ahead in the file stream to get to the data
                readMagicHeader(fis);
                readIncompleteFileMetadata(fis);

                // Read file data, replacing empty data chunks with 'null's
                while ((numBytesRead = fis.read(readChunk)) > -1) {
                    if (chunkAvailability[index]) {
                        chunks.add(new Chunk(index, numBytesRead, readChunk, metadata));
                    } else {
                        chunks.add(null);
                        this.incompleteChunks.add(index);
                    }

                    readChunk = new byte[Chunk.MAX_CHUNK_SIZE];
                    index++;
                }
            }

            fis.close();

            this.chunks = chunks.toArray(this.chunks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addChunk(Chunk chunk) {
        System.out.println("Adding chunk with id " + chunk.getId() + " to file " + chunk.getMetadata().getFileName());

        if (this.chunks[chunk.getId()] == null) {
            this.chunks[chunk.getId()] = chunk;
            this.incompleteChunks.remove(chunk.getId());

            //Complete file!
            if (this.incompleteChunks.size() == 0) {
                System.out.println("File is complete. Saving to " + chunk.getMetadata().getFileName());
                this.isComplete = true;
                save();
            } else {
                // If incomplete, save every 20%
                int numChunks = this.chunks.length;
                int completeChunks = numChunks - this.incompleteChunks.size();

                if (completeChunks - lastSync > (numChunks * 0.2)) {
                    System.out.println("File is " + (int) ((completeChunks / (double) (numChunks)) * 100) + "% complete. Flushing snapshot to " + chunk.getMetadata().getFileName());
                    lastSync = completeChunks;
                    save();
                }
            }
        }
    }

    public void save() {
        FileOutputStream fos = null;
        try {
            this.isComplete = this.incompleteChunks.isEmpty();

            File f = new File(Config.FILES_DIRECTORY + "/" + this.fileName);

            /*
            File f = new File(this.fileName + ((this.isComplete) ? "" : ".remote"));

            if(this.isComplete) {
                new File(this.fileName + ".remote").delete(); //Remove temp file
            }
            */

            if (!f.exists()) {
                File parent = f.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Couldn't create dir: " + parent);
                }

                f.createNewFile();
            }

            fos = new FileOutputStream(Config.FILES_DIRECTORY + "/" + this.fileName);

            if (this.isComplete) {
                for (Chunk c : this.chunks) {
                    fos.write(c.getData());
                }
            } else {
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                fos.write(INCOMPLETE_FILE_MAGIC_HEADER);

                boolean[] chunkAvailability = new boolean[this.chunks.length];

                for (Integer i : this.incompleteChunks) {
                    chunkAvailability[i] = false;
                }

                IncompleteFileMetadata metadata = new IncompleteFileMetadata(chunkAvailability, this.size, this.fileName);
                oos.writeObject(metadata);

                for (Chunk c : this.chunks) {
                    if (c == null) {
                        fos.write(new byte[Chunk.MAX_CHUNK_SIZE]);
                    } else {
                        fos.write(c.getData());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

    public Set<Integer> getIncompleteChunks() {
        return incompleteChunks;
    }
}
