package ece454;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

public abstract class SocketHandlerThread implements Runnable {
    protected final Socket socket;
    protected InputStream socketInputStream = null;
    protected ObjectInputStream messageInputStream = null;

    public SocketHandlerThread(Socket socket) {
        this.socket = socket;
        try {
            socketInputStream = this.socket.getInputStream();
            messageInputStream = new ObjectInputStream(socketInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract void run();
}
