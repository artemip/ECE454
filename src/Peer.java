package ece454p1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Peer {

    private class SocketAcceptor implements Runnable {
        @Override
        public void run() {
            Socket connectedSocket;

            try {
                while(!isClosing()) {
                    connectedSocket = serverSocket.accept();
                    serverHandlerWorkerPool.execute(new SocketHandlerThread(connectedSocket));
                }

                //Cleanup
                serverHandlerWorkerPool.awaitTermination(5, TimeUnit.SECONDS);
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                serverHandlerWorkerPool.shutdownNow();
            }
        }
    }

    private class SocketHandlerThread implements Runnable {
        private final Socket socket;

        public SocketHandlerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            //TODO: Implement communication protocol (recipient-side) here
            InputStream socketInputStream = null;
            ObjectInputStream chunkInputStream = null;
            Chunk readChunk;

            // Attempt to connect to and send a socket a message MAX_SEND_RETRIES times
            for(int i = 0; i < Config.MAX_SEND_RETRIES; ++i) {
                try {
                    socketInputStream = this.socket.getInputStream();
                    chunkInputStream = new ObjectInputStream(socketInputStream);

                    try {
                        Object obj = chunkInputStream.readObject();

                        if(obj instanceof Chunk) {
                            readChunk = (Chunk)obj;
                            // Read the chunk. Now find (or create) the appropriate file and write the chunk
                            DistributedFile distFile = getFileForChunk(readChunk);

                            if(distFile == null) {
                                // No file for this chunk
                                distFile = new DistributedFile(readChunk.getMetadata());
                            } else {
                                if(distFile.isComplete()) {
                                    // Ignore this chunk
                                } else {
                                    if(!distFile.hasChunk(readChunk.getId())) {
                                        distFile.addChunk(readChunk);
                                    }
                                }
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(chunkInputStream != null)
                            chunkInputStream.close();

                        if(socketInputStream != null)
                            socketInputStream.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private final int port;
    private ServerSocket serverSocket;
    private Thread socketAcceptorThread;
    private ExecutorService serverHandlerWorkerPool;
    private boolean closing = false;
    private List<DistributedFile> files;
    private ChunkSender chunkSender;

    public Peer(int port, ChunkSender chunkSender) {
        this.port = port;
        this.files = new ArrayList<DistributedFile>();
        this.chunkSender = chunkSender;

        //Create directory in which to store files
        (new File(Config.FILES_DIRECTORY)).mkdirs();

        try {
            scanFiles();
        } catch (FileNotFoundException e) {
            // Problem scanning files
            e.printStackTrace();
        }

        broadcastFiles();
    }

    private void broadcastFiles() {
        for(DistributedFile f : files) {
            for(Chunk c : f.getChunks()) {
                chunkSender.broadcastChunk(c);
            }
        }
    }

    private void scanFiles() throws FileNotFoundException {
        File filesDir = new File(Config.FILES_DIRECTORY);

        for(File file : filesDir.listFiles()) {
            this.files.add(new DistributedFile(Config.FILES_DIRECTORY + "/" + file.getName()));
        }
    }

    private DistributedFile getFileForChunk(Chunk chunk) {
        for(DistributedFile f : this.files) {
            // We have a file for this chunk
            if(f.getFileName() == chunk.getFileName()) {
                return f;
            }
        }

        return null;
    }

    public void startServerSocket() throws IOException {
        if (serverSocket == null)
            serverSocket = new ServerSocket(port);

        serverHandlerWorkerPool = Executors.newFixedThreadPool(Config.MAX_PEERS - 1);

        socketAcceptorThread = new Thread(new SocketAcceptor());
        socketAcceptorThread.run();
    }

	public int insert(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return ReturnCodes.FILE_NOT_FOUND;
        }

        String newPath = Config.FILES_DIRECTORY + "/" + file.getName();
        File peerFile = new File(newPath);
        DistributedFile newFile;
        try {
            FileUtils.copyFile(file, peerFile);

            // Save as DistributedFile, which splits it up into chunks
            newFile = new DistributedFile(newPath);
        } catch (IOException e) {
            e.printStackTrace();
            return ReturnCodes.FILE_COPY_ERR;
        }

        // Add to list of local files
        files.add(newFile);

        // Send the chunks to all connected peers
        for(Chunk c : newFile.getChunks())
            chunkSender.broadcastChunk(c);

        return ReturnCodes.OK;
    }

	public int query(Status status) {
        //Populate 'status' with information regarding a file
        //PARAMETERS:
        //1) Fraction of file that is available locally
        //2) Fraction of file available in the system
        //3) Least replication level
        //4) Weighted least-replication level

        return ReturnCodes.OK;
    }

	/*
	 * Note that we should have the peer list, so it is not needed as a
	 * parameter
	 */
	public int join() {
        //Sync with all peers: push local files, pull external files (that it does not already have)
        return ReturnCodes.OK;
    }

	public int leave() {
        //Close all sockets
        //Inform all peers of absence
        //Preferred: push out rare file chunks before leaving
        return ReturnCodes.OK;
    }

    public void close() {
        closing = true;
    }

    public boolean isClosing() {
        return closing;
    }
}
