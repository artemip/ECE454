package ece454;

import ece454.util.Config;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class SocketAcceptor implements Runnable {
    private boolean isClosing = false;
    private ServerSocket serverSocket;
    private ExecutorService serverHandlerWorkerPool;
    private ISocketHandlerThreadFactory socketHandlerThreadFactory;

    public SocketAcceptor(ServerSocket serverSocket, ISocketHandlerThreadFactory socketHandlerThreadFactory) {
        this.serverSocket = serverSocket;
        this.socketHandlerThreadFactory = socketHandlerThreadFactory;
        serverHandlerWorkerPool = Executors.newFixedThreadPool(Config.MAX_PEERS - 1);
    }

    @Override
    public void run() {
        Socket connectedSocket;

        try {
            while(isClosing) {
                connectedSocket = serverSocket.accept();
                serverHandlerWorkerPool.execute(socketHandlerThreadFactory.createThread(connectedSocket));
            }

            //Cleanup
            serverHandlerWorkerPool.awaitTermination(5, TimeUnit.SECONDS);
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error occurred when waiting for a socket connection: " + e);
        } catch (InterruptedException e) {
            serverHandlerWorkerPool.shutdownNow();
        }
    }

    public void close() {
        isClosing = true;
    }
}
