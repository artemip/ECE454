package ece454;

import java.net.Socket;

public interface ISocketHandlerThreadFactory {
    public SocketHandlerThread createThread(Socket clientSocket);
}
