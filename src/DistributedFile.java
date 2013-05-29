package ece454p1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class DistributedFile {
    private String path;
    private Chunk[] chunks;

    public DistributedFile(String path) throws FileNotFoundException {
        //TODO: What if the file is not complete?

        this.path = path;
        File file = new File(path);
        String relativePath = Config.FILES_DIRECTORY + "/" + file.getName();

        if(!file.exists()) {
            throw new FileNotFoundException("File at " + path + " does not exist.");
        }

        try {
            int numChunks = (int)((file.length()) / Chunk.MAX_CHUNK_SIZE);
            ArrayList<Chunk> chunks = new ArrayList<Chunk>(numChunks);
            FileInputStream f = new FileInputStream(file);
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
