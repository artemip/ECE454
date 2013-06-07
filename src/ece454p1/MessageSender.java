package ece454p1;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageSender extends Thread {

    private class SocketStream {
        private Socket socket;
        private ObjectOutputStream objectOutputStream;

        private void getOutputStreamFromSocket() {
            try {
                this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            } catch (IOException e) {
                this.objectOutputStream = null;
            }
        }

        public SocketStream(Socket socket) {
            this.socket = socket;
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

        public ObjectOutputStream getObjectOutputStream() {
            return objectOutputStream;
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

            synchronized (peerSocketStream.getSocket()) {
                ObjectOutputStream chunkOutputStream = null;

                // Attempt to connect to and send a socket a message MAX_SEND_RETRIES times
                for(int i = 0; i < Config.MAX_SEND_RETRIES; ++i) {
                    try {
                        Socket peerSocket = peerSocketStream.getSocket();
                        while(peerSocket == null || peerSocket.isClosed() || !peerSocket.isConnected()) {
                            peerSocketStream.resetSocket(recipient); //Re-connect

                            //Last try, didn't work
                            if(++i == Config.MAX_SEND_RETRIES - 1) {
                                return;
                            }
                        }

                        chunkOutputStream = peerSocketStream.getObjectOutputStream();

                        chunkOutputStream.writeObject(message);
                        chunkOutputStream.flush();
                        i = Config.MAX_SEND_RETRIES;
                    } catch (IOException e) {
                        System.err.println("Could not send message to host " + recipient.getFullAddress() + ": " + e);

                        //Last try, didn't work
                        if(i == Config.MAX_SEND_RETRIES - 1) {
                            return;
                        }
                    }
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
                s = new Socket();
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
            e.printStackTrace();
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
            senderThreadPool.shutdown();
        }
    }
}
