package ece454p1;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
            InputStream socketInputStream = null;
            ObjectInputStream chunkInputStream = null;

            // Attempt to connect to and send a socket a message MAX_SEND_RETRIES times
            for(int i = 0; i < Config.MAX_SEND_RETRIES; ++i) {
                try {
                    socketInputStream = this.socket.getInputStream();
                    chunkInputStream = new ObjectInputStream(socketInputStream);

                    try {
                        Object obj = chunkInputStream.readObject();

                        if(obj instanceof ChunkMessage) {
                            ChunkMessage msg = (ChunkMessage)obj;
                            Chunk chunk = msg.getChunk();

                            // Read the chunk. Now find (or create) the appropriate file and write the chunk
                            DistributedFile distFile = getFileForChunk(chunk);

                            if(distFile == null) {
                                // No file for this chunk
                                distFile = new DistributedFile(chunk.getMetadata());
                                files.put(distFile.getFileName(), distFile);
                                distFile.addChunk(chunk);
                            } else {
                                if(distFile.isComplete()) {
                                    // Ignore this chunk
                                } else {
                                    if(!distFile.hasChunk(chunk.getId())) {
                                        distFile.addChunk(chunk);
                                    }
                                }
                            }
                        } else if(obj instanceof PullMessage) {
                            // Push all chunks to sender
                            InetSocketAddress addr = (InetSocketAddress)socket.getRemoteSocketAddress();
                            PeerDefinition pd = PeersList.getPeerByAddress(addr.getHostName(), addr.getPort());

                            sendAllChunksToPeer(pd);
                        } else if(obj instanceof QueryMessage) {
                            //TODO: do this
                            // Return query to sender
                            InetSocketAddress addr = (InetSocketAddress)socket.getRemoteSocketAddress();
                            PeerDefinition pd = PeersList.getPeerByAddress(addr.getHostName(), addr.getPort());

                            //file = msg.getFile()
                            //status = ....
                            //populate query data
                            //messageSender.sendMessage(new QueryMessage(pd,));
                        } else {
                            System.err.println("Received message of unknown type");
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println("Received message of unknown type");
                    }
                } catch (IOException e) {
                    System.err.println("Problem reading an object from the socket: " + e);
                } finally {
                    try {
                        if(chunkInputStream != null)
                            chunkInputStream.close();

                        if(socketInputStream != null)
                            socketInputStream.close();

                    } catch (IOException e) {
                        System.err.println("Problems closing socket stream: " + e);
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
    private Map<String, DistributedFile> files;
    private MessageSender messageSender;
    private PeerDefinition definition;

    public Peer(int port, MessageSender messageSender) {
        this.port = port;
        this.files = new ConcurrentHashMap<String, DistributedFile>();
        this.messageSender = messageSender;

        //Create directory in which to store files
        (new File(Config.FILES_DIRECTORY)).mkdirs();

        try {
            scanFiles();
        } catch (FileNotFoundException e) {
            // Problem scanning files
            e.printStackTrace();
        }
    }

    private void broadcastFiles() {
        for(DistributedFile f : files.values()) {
            for(Chunk c : f.getChunks()) {
                ChunkMessage.broadcast(c, messageSender);
            }
        }
    }

    private void sendAllChunksToPeer(PeerDefinition recipient) {
        for(DistributedFile f : files.values()) {
            for(Chunk c : f.getChunks()) {
                messageSender.sendMessage(new ChunkMessage(c, recipient));
            }
        }
    }

    private void scanFiles() throws FileNotFoundException {
        File filesDir = new File(Config.FILES_DIRECTORY);

        for(File file : filesDir.listFiles()) {
            DistributedFile distFile = new DistributedFile(Config.FILES_DIRECTORY + "/" + file.getName());
            this.files.put(distFile.getFileName(), distFile);
        }
    }

    private void saveFiles() {
        for(DistributedFile f : files.values()) {
            f.save();
        }
    }

    private DistributedFile getFileForChunk(Chunk chunk) {
        return files.get(chunk.getMetadata().getFileName());
    }

    public void startServerSocket() throws IOException {
        if (serverSocket == null)
            serverSocket = new ServerSocket(port);

        serverHandlerWorkerPool = Executors.newFixedThreadPool(Config.MAX_PEERS - 1);

        socketAcceptorThread = new Thread(new SocketAcceptor());
        socketAcceptorThread.start();
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
        files.put(newFile.getFileName(), newFile);

        // Send the chunks to all connected peers
        for(Chunk c : newFile.getChunks())
            ChunkMessage.broadcast(c, messageSender);

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
        try {
            this.startServerSocket();
        } catch (IOException e) {
            System.err.println("Could not open socket connection on port " + port + ": " + e.toString());
        }

        messageSender.start();

        broadcastFiles();

        // Pull files
        PullMessage.broadcast(messageSender);

        return ReturnCodes.OK;
    }

	public int leave() {
        //Inform all peers of absence
        //Preferred: push out rare file chunks before leaving
        //Close all sockets
        close();
        messageSender.shutdown();
        return ReturnCodes.OK;
    }

    public void close() {
        closing = true;
    }

    public boolean isClosing() {
        return closing;
    }
}
