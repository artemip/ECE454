package ece454p1;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
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
                    while(SocketUtils.isSocketOpen(this.socket)) {
                        try {
                            Object obj = messageInputStream.readObject();

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
                            }
                            else if(obj instanceof PullMessage) {
                                // Push all chunks to sender
                                InetSocketAddress addr = (InetSocketAddress)socket.getRemoteSocketAddress();
                                PeerDefinition pd = PeersList.getPeerByAddress(addr.getHostName(), addr.getPort());

                                sendAllChunksToPeer(pd);
                            }
                            else if(obj instanceof QueryMessage) {
                                InetSocketAddress addr = (InetSocketAddress)socket.getRemoteSocketAddress();
                                PeerDefinition pd = PeersList.getPeerByAddress(addr.getHostName(), addr.getPort());

                                // got query message, build PeerFileListInfo Message to send back
                                PeerFileListInfo fListInfo = new PeerFileListInfo(getDistributedFileList());
                                FileListInfoMessage.replyToQuery(messageSender, pd, fListInfo);

                            }
                            else if (obj instanceof FileListInfoMessage){
                                //list of fileListInfo to use in Query()
                                InetSocketAddress addr = (InetSocketAddress)socket.getRemoteSocketAddress();
                                PeerDefinition pd = PeersList.getPeerByAddress(addr.getHostName(), addr.getPort());

                                FileListInfoMessage fListInfoMsg = (FileListInfoMessage)obj;
                                addPeerFileList(pd, fListInfoMsg.getPeerFileListInfo());
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
    private ServerSocket serverSocket;
    private Thread socketAcceptorThread;
    private ExecutorService serverHandlerWorkerPool;
    private boolean closing = false;
    private Map<String, DistributedFile> files;
    private MessageSender messageSender;
    private Map<PeerDefinition, PeerFileListInfo> peerFileLists;
    private Hashtable<String, int[]> globalFileList;
    
    public Peer(int port, MessageSender messageSender) {
        this.port = port;
        this.files = new ConcurrentHashMap<String, DistributedFile>();
        this.peerFileLists = new ConcurrentHashMap<PeerDefinition, PeerFileListInfo>();
        this.globalFileList = new Hashtable<String,int[]>();
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
        
    	this.peerFileLists.clear();
        this.globalFileList.clear();
        
        //Query all peers to build FileListInfo
        //messageSender.start();
        QueryMessage.broadcast(messageSender);
        System.out.println("Query sent");
                
        //first populate global list with local files
        PeerFileListInfo ownFileList = new PeerFileListInfo(getDistributedFileList());
                        
        //calculate status.local 
        for(DistributedFile f : files.values()) {
        	int numTotalChunks = f.getChunks().length;
            int completeChunks = numTotalChunks - f.getIncompleteChunks().size();

        	status.addLocalFile(f.getFileName(), (float)completeChunks/numTotalChunks);
    	}        
        
        mergePeerFileLists(ownFileList);

        //wait for all of the responses
        System.out.println("Waiting for peer query responses ..................................");
        while(peerFileLists.size() != (PeersList.getPeers().size())){}
        System.out.println("All peer responses received");
        
        //populate global list with peer file lists
        for (PeerFileListInfo p: peerFileLists.values()){
        	mergePeerFileLists(p);
        }
              	
        //populate rest of status
		Iterator<Map.Entry<String, int[]>> it = globalFileList.entrySet().iterator();
		while (it.hasNext()){
			Map.Entry<String, int[]> entry = it.next();
			int fileChunksInSystem = 0;
			int totalNumChunks = 0;
			int leastReplicatedChunkIndex = 0;
			
			for (int i=0; i<entry.getValue().length; i++){
				if (entry.getValue()[i] > 0){
					fileChunksInSystem++;
					totalNumChunks += entry.getValue()[i];
					if (entry.getValue()[i] < entry.getValue()[leastReplicatedChunkIndex]){
						leastReplicatedChunkIndex = i;
					}
				}
				else{
					leastReplicatedChunkIndex = i;
				}
			}
			float fileFractionInSystem = fileChunksInSystem/entry.getValue().length;
			float weightedReplication = totalNumChunks/entry.getValue().length;
			status.addSystemFile(entry.getKey(), fileFractionInSystem);
			status.addLeastReplicatedChunk(entry.getKey(), leastReplicatedChunkIndex);
			status.addWeightedLeastReplicated(entry.getKey(), weightedReplication);
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

	public void addPeerFileList(PeerDefinition source, PeerFileListInfo peerFileList) {
		this.peerFileLists.put(source, peerFileList);
	}
	
	private void mergePeerFileLists(PeerFileListInfo peerFileList){
					
		//cycle through each peer file hash table entry and update global file list
		Iterator<Map.Entry<String, boolean[]>> it = peerFileList.getPeerFileHashTable().entrySet().iterator();
		while (it.hasNext()){
			Map.Entry<String, boolean[]> entry = it.next();
			
			int[] tempIntArray = new int[entry.getValue().length];
			
			for (int i=0; i<entry.getValue().length; i++){
				int temp = 0;
				
				//if file is already on the global list, add to existing chunk values
				if (this.globalFileList.containsKey(entry.getKey())){
					temp += entry.getValue()[i]? 1: 0;
					tempIntArray[i] = globalFileList.get(entry.getKey())[i] + temp;
				}
				else{
					tempIntArray[i] = entry.getValue()[i]? 1: 0;						
				}					
			}
			this.globalFileList.put(entry.getKey(), tempIntArray);
		}
	}
	
}
