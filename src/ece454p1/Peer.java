package ece454p1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        private InputStream socketInputStream = null;
        private ObjectInputStream messageInputStream = null;

        public SocketHandlerThread(Socket socket) {
            this.socket = socket;
            try {
                socketInputStream = this.socket.getInputStream();
                messageInputStream = new ObjectInputStream(socketInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                synchronized (this.socket) {
                    PeerDefinition sender = null;

                    while(SocketUtils.isSocketOpen(this.socket)) {
                        try {
                            Object obj = messageInputStream.readObject();

                            if(sender == null) {
                                sender = PeersList.getPeerById(((Message)obj).getSenderId());
                                if(sender == null) {
                                    System.err.println("Opened socket connection with unknown host. Closing connection...");
                                    return;
                                }
                            }

                            if(obj instanceof ChunkMessage) {
                                System.out.println("Received chunk from " + sender.getFullAddress());
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
                                        System.out.println("Ignoring chunk...");
                                    } else {
                                        if(!distFile.hasChunk(chunk.getId())) {
                                            distFile.addChunk(chunk);
                                        }
                                    }
                                }
                            }
                            else if(obj instanceof PullMessage) {
                                // Push all chunks to sender
                                System.out.println("Received pull request from " + sender.getFullAddress());
                                sendAllChunksToPeer(sender);
                            }
                            else if(obj instanceof QueryMessage) {
                                // got query message, build PeerFileListInfo Message to send back
                                System.out.println("Received query message from " + sender.getFullAddress());
                                PeerFileListInfo fListInfo = new PeerFileListInfo(getDistributedFileList());
                                messageSender.sendMessage(new FileListInfoMessage(sender, fListInfo, id));
                            }
                            else if (obj instanceof FileListInfoMessage){
                                //list of fileListInfo to use in Query()
                                System.out.println("Received file info request message from " + sender.getFullAddress());
                                FileListInfoMessage fListInfoMsg = (FileListInfoMessage)obj;
                                addPeerFileList(fListInfoMsg.getPeerFileListInfo());
                            } else {
                                System.err.println("Received message of unknown type");
                            }
                        } catch (ClassNotFoundException e) {
                            System.err.println("Received message of unknown type");
                        } catch (IOException e) {
                            System.err.println("Problems reading object from socket: " + e);
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            } finally {
                try {
                    messageInputStream.close();
                    socketInputStream.close();
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Problems closing object input stream: " + e);
                }
            }
        }
    }

    private final int port;
    private final int id;
    private ServerSocket serverSocket;
    private Thread socketAcceptorThread;
    private ExecutorService serverHandlerWorkerPool;
    private boolean closing = false;
    private Map<String, DistributedFile> files;
    private MessageSender messageSender;
    private List<PeerFileListInfo> peerFileLists;
    private int peerResponseCounter;
    
    public Peer(int id, int port, MessageSender messageSender) {
        this.id = id;
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
                ChunkMessage.broadcast(c, messageSender, id);
            }
        }
    }

    private void sendAllChunksToPeer(PeerDefinition recipient) {
        for(DistributedFile f : files.values()) {
            for(Chunk c : f.getChunks()) {
                messageSender.sendMessage(new ChunkMessage(c, recipient, id));
            }
        }
    }

    private Collection<DistributedFile> getDistributedFileList(){
    	return this.files.values();
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
            ChunkMessage.broadcast(c, messageSender, id);

        return ReturnCodes.OK;
    }

	public int query(Status status) {
        //Populate 'status' with information regarding a file
        //PARAMETERS:
        //1) Fraction of file that is available locally
        //2) Fraction of file available in the system
        //3) Least replication level
        //4) Weighted least-replication level
        try {
            this.startServerSocket();
        } catch (IOException e) {
            System.err.println("Could not open socket connection on port " + port + ": " + e.toString());
        }
        
        clearPeerFileLists();
        
        //Query all peers to build FileListInfo
        messageSender.start();
        QueryMessage.broadcast(messageSender, id);
        
        int numFiles;
        
    	/*
    	 * The fraction of the file present locally (= chunks on this peer/total
    	 * number chunks in the file)
    	 */
        float[] local;
        
    	/*
    	 * The fraction of the file present in the system 
    	 * (= chunks in the * system/total number chunks in the file) 
    	 * (Note that if a chunk is present twice, 
    	 * it doesn't get counted twice; this is simply intended to find out
    	 * if we have the whole file in the system; 
    	 * given that a file must be added at a peer, 
    	 * think about why this number would ever not be 1.)
    	 */
        float[] system;
        
    	/*
    	 * Sum by chunk over all peers; the minimum of this number is the least
    	 * replicated chunk, and thus represents the least level of 
    	 * replication of  the file
    	 */        
        int[] leastReplication;
        
    	/*
    	 * Sum all chunks in all peers; 
    	 * dived this by the number of chunks in the file; 
    	 * this is the average level of replication of the file
    	 */
        float[] weightedLeastReplication;
        
        //calculate local[]
        local = new float[files.size()];
        int filenum = 0;
        for(DistributedFile f : files.values()) {
        	int numTotalChunks = f.getChunks().length;
            int completeChunks = numTotalChunks - f.getIsComplete().size();
            
    		local[filenum] = completeChunks/numTotalChunks;
        	filenum++;
    	}

        //populate rest of status
        List<String> fnames = new ArrayList<String>();
        List<boolean[]> ca = new ArrayList<boolean[]>();
        
    	//cycle through all of the peerFileLists
        for (PeerFileListInfo p: peerFileLists){
        	//cycle through all of the files on each fileList
        	for(String fn : p.getFileNames()){

        	}
        }

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
        PullMessage.broadcast(messageSender, id);

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

    public int getId() {
        return this.id;
    }

    public void close() {
        closing = true;
    }

    public boolean isClosing() {
        return closing;
    }

	public void addPeerFileList(PeerFileListInfo peerFileList) {
		this.peerFileLists.add(peerFileList);
	}
	
	public void clearPeerFileLists(){
		peerFileLists.clear();
	}
}
