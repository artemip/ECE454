package ece454;

import ece454.messages.*;
import ece454.util.SocketUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class NodeAddressServer {
    public static final PeerDefinition NAS_DEFINITION = new PeerDefinition("localhost", 9000, 0);

    private class NASSocketHandlerThread extends SocketHandlerThread {

        public NASSocketHandlerThread(Socket socket) {
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

                            if(obj instanceof NodeListMessage) {
                                System.out.println("Received node list request from " + sender.getFullAddress());

                                messageSender.sendMessage(new NodeListMessage(sender, 0, (PeerDefinition[])idToPeerMap.values().toArray()));
                            } else {
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

    private class NASSocketHandlerThreadFactory implements ISocketHandlerThreadFactory {
        @Override
        public SocketHandlerThread createThread(Socket clientSocket) {
            return new NASSocketHandlerThread(clientSocket);
        }
    }

    private Map<Integer, PeerDefinition> idToPeerMap;
    private ServerSocket serverSocket;
    private Thread socketAcceptorThread;
    private MessageSender messageSender;

    public NodeAddressServer() {
        this.idToPeerMap = new HashMap<Integer, PeerDefinition>();
    }

    public void addPeer(int id, PeerDefinition definition) {
        idToPeerMap.put(id, definition);
    }

    public void removePeer(int id) {
        idToPeerMap.remove(id);
    }

    public PeerDefinition getPeer(int id) {
        return idToPeerMap.get(id);
    }

    public void join() {
        if(messageSender == null)
            messageSender = new MessageSender();

        try {
            this.startServerSocket();
        } catch (IOException e) {
            System.err.println("Could not open NAS socket connection: " + e.toString());
        }

        messageSender.start();
    }

    public void startServerSocket() throws IOException {
        if (serverSocket == null)
            serverSocket = new ServerSocket(NAS_DEFINITION.getPort());

        socketAcceptorThread = new Thread(new SocketAcceptor(serverSocket, new NASSocketHandlerThreadFactory()));
        socketAcceptorThread.start();
    }
}
