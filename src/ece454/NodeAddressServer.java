package ece454;

import ece454.messages.*;
import ece454.util.Config;
import ece454.util.SocketUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class NodeAddressServer {
    public static final PeerDefinition NAS_DEFINITION = new PeerDefinition("127.0.0.1", 8000, 0);
    private static final List<DisabledPeer> disabledPeers = new ArrayList<DisabledPeer>();

    private class DisabledPeer {
        private int id;
        private long timeOfDeparture;
        private List<Message> queuedMessages;

        DisabledPeer(int id, long timeOfDeparture) {
            this.id = id;
            this.timeOfDeparture = timeOfDeparture;
            this.queuedMessages = new ArrayList<Message>();
        }

        public int getId() {
            return id;
        }

        public long getTimeOfDeparture() {
            return timeOfDeparture;
        }

        public void addMessage(Message m) {
            queuedMessages.add(m);
        }

        public void sendMessages(MessageSender sender) {
            for(Message m : queuedMessages) {
                sender.sendMessage(m);
            }
        }
    }

    private class NASSocketHandlerThread extends SocketHandlerThread {

        public NASSocketHandlerThread(Socket socket) {
            super(socket);
        }

        @Override
        public void run() {
            try {
                synchronized (this.socket) {
                    while (SocketUtils.isSocketOpen(this.socket)) {
                        try {
                            Object obj = messageInputStream.readObject();

                            if (obj instanceof NodeListMessage) {
                                NodeListMessage msg = (NodeListMessage) obj;
                                PeerDefinition sender = null;

                                if ((sender = PeersList.getPeerById(msg.getSenderId())) == null) {
                                    // New peer
                                    sender = new PeerDefinition(this.socket.getInetAddress().getHostAddress(), msg.getSenderPort(), msg.getSenderId());
                                    System.out.println("Found new peer at " + sender.getFullAddress());

                                    // Add the sender to the peers list
                                    idToPeerMap.put(msg.getSenderId(), sender);
                                    PeersList.addPeer(sender);
                                    messageSender.addPeerSocket(sender);

                                    int removeIndex = -1;

                                    for(int i = 0; i < disabledPeers.size(); i++) {
                                        DisabledPeer p = disabledPeers.get(i);
                                        if(p.getId() == sender.getId()) {
                                            p.sendMessages(messageSender);
                                            removeIndex = i;
                                            break;
                                        }
                                    }

                                    if(removeIndex > -1)
                                        disabledPeers.remove(removeIndex);
                                }

                                //Broadcast new list of peers to all peers
                                NodeListMessage.broadcast(
                                        NAS_DEFINITION.getPort(),
                                        idToPeerMap.values().toArray(
                                                new PeerDefinition[idToPeerMap.values().size()]),
                                        messageSender,
                                        NAS_DEFINITION.getId());
                            } else if(obj instanceof LeaveMessage) {
                                LeaveMessage msg = (LeaveMessage)obj;

                                idToPeerMap.remove(msg.getSenderId());
                                PeersList.removePeer(msg.getSenderId());
                                messageSender.removePeerSocket(msg.getSenderId());

                                NodeListMessage.broadcast(
                                        NAS_DEFINITION.getPort(),
                                        idToPeerMap.values().toArray(
                                                new PeerDefinition[idToPeerMap.values().size()]),
                                        messageSender,
                                        NAS_DEFINITION.getId());

                                disabledPeers.add(new DisabledPeer(msg.getSenderId(), new Date().getTime()));
                            } else {
                                for(DisabledPeer p : disabledPeers) {
                                    if(obj instanceof ChunkMessage) {
                                        p.addMessage((ChunkMessage)obj);
                                    } else if(obj instanceof DeleteMessage) {
                                        p.addMessage((DeleteMessage)obj);
                                    }
                                }
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
        this.idToPeerMap.put(0, NAS_DEFINITION);
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
        if (messageSender == null)
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
