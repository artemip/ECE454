package ece454p1;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageSender extends Thread {
    private class MessageThread implements Runnable {
        private Message message;

        public MessageThread(Message message) {
            this.message = message;
        }

        @Override
        public void run() {
            PeerDefinition recipient = message.getRecipient();
            Socket peerSocket = peerSocketsMap.get(recipient);

            synchronized (peerSocket) {
                OutputStream socketOutputStream = null;
                ObjectOutputStream chunkOutputStream = null;

                // Attempt to connect to and send a socket a message MAX_SEND_RETRIES times
                for(int i = 0; i < Config.MAX_SEND_RETRIES; ++i) {
                    try {
                        if(peerSocket == null || peerSocket.isClosed() || !peerSocket.isConnected()) {
                            peerSocket = new Socket(recipient.getIPAddress(), recipient.getPort());
                            peerSocketsMap.put(recipient, peerSocket); //Save new connection
                        }

                        socketOutputStream = peerSocket.getOutputStream();
                        chunkOutputStream = new ObjectOutputStream(socketOutputStream);

                        chunkOutputStream.writeObject(message);
                        i = Config.MAX_SEND_RETRIES;
                    } catch (IOException e) {
                        System.err.println("Could not send message to host " + recipient.getFullAddress() + ": " + e);

                        //Last try, didn't work
                        if(i == Config.MAX_SEND_RETRIES - 1) {
                            return;
                        }
                    } finally {
                        try {
                            if(chunkOutputStream != null)
                                chunkOutputStream.close();

                            if(socketOutputStream != null)
                                socketOutputStream.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private ConcurrentLinkedQueue<Message> messagesToSend;
    private ExecutorService senderThreadPool;
    private final Map<PeerDefinition, Socket> peerSocketsMap;
    private boolean stopSending = false;

    public MessageSender() {
        peerSocketsMap = new ConcurrentHashMap<PeerDefinition, Socket>();
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

            peerSocketsMap.put(pd, s);
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
