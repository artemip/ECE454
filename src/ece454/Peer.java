package ece454;

import ece454.messages.*;
import ece454.storage.Chunk;
import ece454.storage.DistributedFile;
import ece454.util.Config;
import ece454.util.FileUtils;
import ece454.util.SocketUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;

import java.io.*;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {

    private class PeerSocketHandlerThread extends SocketHandlerThread {

        public PeerSocketHandlerThread(Socket socket) {
            super(socket);
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

                                //messageSender.sendMessage(new FileListInfoMessage(sender, fListInfo, id));
                            }
                            else if(obj instanceof NodeListMessage) {
                                NodeListMessage msg = (NodeListMessage)obj;
                                PeersList.initialize(msg.getNodeList());

                                synchronize();
                            }
                            else {
                                System.err.println("Received message of unknown type");
                            }
                        } catch (ClassNotFoundException e) {
                            System.err.println("Received message of unknown type");
                        } catch (IOException e) {
                            System.err.println("Problems reading object from socket - Peer is likely down: " + e);
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

    private class PeerSocketHandlerThreadFactory implements ISocketHandlerThreadFactory {

        @Override
        public SocketHandlerThread createThread(Socket clientSocket) {
            return new PeerSocketHandlerThread(clientSocket);
        }
    }

    private class PeerFileChangeListener implements FileListener {

        @Override
        public void fileCreated(FileChangeEvent fileChangeEvent) throws Exception {
            if(isNewFile(fileChangeEvent.getFile())) {
                insert(fileChangeEvent.getFile().getURL().getPath());
            }
        }

        @Override
        public void fileDeleted(FileChangeEvent fileChangeEvent) throws Exception {
            //TODO: Delete a file
        }

        @Override
        public void fileChanged(FileChangeEvent fileChangeEvent) throws Exception {
            //TODO: Changed file should propogate to all other peers
        }

        private boolean isNewFile(FileObject fileObject) {
            File f;

            try {
                f = new File(fileObject.getURL().getPath());
            } catch (FileSystemException e) {
                e.printStackTrace();
                return true;
            }

            return !files.containsKey(FileUtils.getRelativePath(f, Config.FILES_DIRECTORY));
        }
    }

    private final int port;
    private final int id;
    private ServerSocket serverSocket;
    private Thread socketAcceptorThread;
    private boolean closing = false;
    private Map<String, DistributedFile> files;
    private MessageSender messageSender;
    private Map<PeerDefinition, PeerFileListInfo> peerFileLists;
    private Hashtable<String, int[]> globalFileList;
    
    public Peer(int id, int port) {
        this.id = id;
        this.port = port;
        this.files = new ConcurrentHashMap<String, DistributedFile>();
        this.peerFileLists = new ConcurrentHashMap<PeerDefinition, PeerFileListInfo>();
        this.globalFileList = new Hashtable<String,int[]>();

        //Create directory in which to store files
        (new File(Config.FILES_DIRECTORY_NAME)).mkdirs();

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
        Queue<File> files = new LinkedList<File>();
        File file = Config.FILES_DIRECTORY;

        files.addAll(Arrays.asList(file.listFiles()));

        while(!files.isEmpty()) {
            file = files.poll();
            if(file.isDirectory()) {
                files.addAll(Arrays.asList(file.listFiles()));
            } else {
                DistributedFile distFile = new DistributedFile(FileUtils.getRelativePath(file, Config.FILES_DIRECTORY));
                this.files.put(distFile.getFileName(), distFile);
            }
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

        socketAcceptorThread = new Thread(new SocketAcceptor(serverSocket, new PeerSocketHandlerThreadFactory()));
        socketAcceptorThread.start();
    }

	public void insert(String filename) {

        File file = new File(filename);

        if (!file.exists()) {
            return;
        }

        String newPath = FileUtils.getRelativePath(file, Config.FILES_DIRECTORY);
        DistributedFile newFile;

        try {
            newFile = new DistributedFile(newPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Add to list of local files
        files.put(newFile.getFileName(), newFile);

        // Send the chunks to all connected peers
        for(Chunk c : newFile.getChunks())
            ChunkMessage.broadcast(c, messageSender, id);

    }

    public void synchronize() {
        // Broadcast local files
        broadcastFiles();

        // Pull external files
        PullMessage.broadcast(messageSender, id);
    }

    public void watchDirectory() {
        FileSystemManager manager = null;
        FileObject file = null;

        try {
            manager = VFS.getManager();
            file = manager.resolveFile(Config.FILES_DIRECTORY_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DefaultFileMonitor fm = new DefaultFileMonitor(new PeerFileChangeListener());
        fm.setRecursive(true);
        fm.setDelay(1000);
        fm.addFile(file);
        fm.start();
    }

	public void join() {
        if(messageSender == null)
            messageSender = new MessageSender();

        try {
            this.startServerSocket();
        } catch (IOException e) {
            System.err.println("Could not open socket connection on port " + port + ": " + e.toString());
        }

        messageSender.start();
        messageSender.sendMessage(new NodeListMessage(NodeAddressServer.NAS_DEFINITION, id, port, null));
    }

	public void leave() {
        //Inform all peers of absence
        //Preferred: push out rare file chunks before leaving
        //Close all sockets

        messageSender.shutdown();
        closing = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = null;
        messageSender = null;
    }

    public int getId() {
        return this.id;
    }
}
