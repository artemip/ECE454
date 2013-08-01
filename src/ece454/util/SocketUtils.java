package ece454.util;

import java.net.Socket;

public class SocketUtils {
    public static boolean isSocketOpen(Socket socket) {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}
