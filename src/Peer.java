package ece454p1;

//TODO: can we do without a programming API? Can we use socket message-passing exclusively?

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Peer {

    private class SocketAcceptorThread extends Thread {
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

    private class SocketHandlerThread extends Thread {
        private final Socket socket;

        public SocketHandlerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            
            //TODO: Implement communication protocol (recipient-side) here
        }
    }

    private final int port;
    private ServerSocket serverSocket;
    private Thread socketAcceptorThread;
    private ExecutorService serverHandlerWorkerPool;
    private boolean closing = false;

    public Peer(int port) {
        this.port = port;

        //Create directory in which to store files
        (new File(Config.FILES_DIRECTORY)).mkdirs();
    }

    public void startServerSocket() throws IOException {
        if (serverSocket == null)
            serverSocket = new ServerSocket(port);

        serverHandlerWorkerPool = Executors.newFixedThreadPool(Config.MAX_PEERS - 1);

        socketAcceptorThread = new SocketAcceptorThread();
        socketAcceptorThread.run();
    }

	public int insert(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return ReturnCodes.FILE_NOT_FOUND;
        }

        File peerFile = new File(Config.FILES_DIRECTORY + "/" + file.getName());
        try {
            FileUtils.copyFile(file, peerFile);
        } catch (IOException e) {
            e.printStackTrace();
            return ReturnCodes.FILE_COPY_ERR;
        }

        //Push out the file to all other peers (split into chunks, send, and distribute among all peers (somehow))
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
