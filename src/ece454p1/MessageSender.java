package ece454p1;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

public class MessageSender extends Thread {
    private class SocketStream {
        private Socket socket = null;
        private ObjectOutputStream messageOutputStream = null;
        private OutputStream socketOutputStream = null;

        private void getOutputStreamFromSocket() {
            try {
                socketOutputStream = socket.getOutputStream();
                messageOutputStream = new ObjectOutputStream(socketOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public SocketStream(Socket socket) {
            this.socket = socket;
            if(SocketUtils.isSocketOpen(this.socket))
                getOutputStreamFromSocket();
        }

        public void resetSocket(PeerDefinition server) {
            try {
                this.socket = new Socket(server.getIPAddress(), server.getPort());
                getOutputStreamFromSocket();
            } catch (IOException e) {
                this.socket = null;
            }
        }

        public Socket getSocket() {
            return socket;
        }

        public void writeMessage(Message message) throws IOException {
            messageOutputStream.writeObject(message);
        }
    }

    private class MessageThread implements Runnable {
        private Message message;

        public MessageThread(Message message) {
            this.message = message;
        }

        @Override
        public void run() {
            PeerDefinition recipient = message.getRecipient();
            SocketStream peerSocketStream = peerSocketsMap.get(recipient);
            Socket peerSocket = peerSocketStream.getSocket();

            synchronized (peerSocket) {
                // Attempt to connect to and send a socket a message
                try {
                    if(!SocketUtils.isSocketOpen(peerSocket)) {
                        peerSocketStream.resetSocket(recipient); //Re-connect
                    }

                    if(SocketUtils.isSocketOpen(peerSocket))
                        peerSocketStream.writeMessage(message);
                } catch (IOException e) {
                    System.err.println("Could not send message to host " + recipient.getFullAddress() + ": " + e);

                    // Reset after unsuccessful connection attempt
                    peerSocketStream.resetSocket(recipient);
                }
            }
        }
    }

    private ConcurrentLinkedQueue<Message> messagesToSend;
    private ExecutorService senderThreadPool;
    private final Map<PeerDefinition, SocketStream> peerSocketsMap;
    private boolean stopSending = false;

    public MessageSender() {
        peerSocketsMap = new ConcurrentHashMap<PeerDefinition, SocketStream>();
        messagesToSend = new ConcurrentLinkedQueue<Message>();
    }

    @Override
    public void start() {
        senderThreadPool = Executors.newFixedThreadPool(Config.NUM_USABLE_CORES);

        for(PeerDefinition pd : PeersList.getPeers()) {
            Socket s = null;
            try {
                s = new Socket(pd.getIPAddress(), pd.getPort());
            } catch (IOException e) {
                System.err.println("Host at " + pd.getFullAddress() + " has not been started. Cannot establish socket connection.");
            }

            peerSocketsMap.put(pd, new SocketStream(s));
        }

        super.start();
    }

    @Override
    public void run() {
        Message msg;
        try {
            while(!stopSending) {
                //Process work queue

                synchronized (this) {
                    // While nothing to process, wait
                    while((msg = messagesToSend.poll()) == null) {
                        wait();
                    }
                }

                senderThreadPool.submit(new MessageSender.MessageThread(msg));
            }
        } catch (InterruptedException e) {
            System.err.println("Exiting work queue loop");
        }
    }

    synchronized private void wakeup() {
        notifyAll();
    }

    public void sendMessage(Message message) {
        messagesToSend.add(message);
        wakeup();
    }

    public void shutdown() {
        stopSending = true;
        wakeup();
        this.interrupt();

        if(senderThreadPool != null) {
            try {
                senderThreadPool.awaitTermination(1L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
