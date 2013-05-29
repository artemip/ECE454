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

public class ChunkSender extends Thread {

    private class ChunkMessage {
        private Chunk chunk;
        private PeerDefinition recipient;

        public ChunkMessage(Chunk chunk, PeerDefinition recipient) {
            this.chunk = chunk;
            this.recipient = recipient;
        }

        public Chunk getChunk() {
            return chunk;
        }

        public PeerDefinition getRecipient() {
            return recipient;
        }
    }

    private class ChunkSenderThread implements Runnable {

        private ChunkMessage chunkMessage;

        public ChunkSenderThread(ChunkMessage chunkMessage) {
            this.chunkMessage = chunkMessage;
        }
        @Override
        public void run() {
            PeerDefinition recipient = chunkMessage.getRecipient();
            Socket peerSocket = peerSocketsMap.get(recipient);

            synchronized (peerSocket) {
                OutputStream socketOutputStream = null;
                ObjectOutputStream chunkOutputStream = null;

                // Attempt to connect to and send a socket a message MAX_SEND_RETRIES times
                for(int i = 0; i < Config.MAX_SEND_RETRIES; ++i) {
                    try {
                        if(peerSocket == null || peerSocket.isClosed()) {
                            peerSocket = new Socket(recipient.getIPAddress(), recipient.getPort());
                        }

                        socketOutputStream = peerSocket.getOutputStream();
                        chunkOutputStream = new ObjectOutputStream(socketOutputStream);

                        chunkOutputStream.writeObject(chunkMessage.chunk);
                    } catch (IOException e) {
                        e.printStackTrace();

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

    private ConcurrentLinkedQueue<ChunkMessage> chunksToSend;
    private ExecutorService senderThreadPool;
    private final Map<PeerDefinition, Socket> peerSocketsMap;
    private boolean stopSending = false;

    public ChunkSender() {
        peerSocketsMap = new ConcurrentHashMap<PeerDefinition, Socket>();
    }

    @Override
    public void start() {
        senderThreadPool = Executors.newFixedThreadPool(Config.NUM_USABLE_CORES);

        for(PeerDefinition pd : PeersList.getPeers()) {
            Socket s = null;
            try {
                s = new Socket(pd.getIPAddress(), pd.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }

            peerSocketsMap.put(pd, s);
        }

        super.start();
    }

    @Override
    public void run() {
        ChunkMessage chunk;
        try {
            while(!stopSending) {
                //Process work queue

                synchronized (this) {
                    // While nothing to process, wait
                    while((chunk = chunksToSend.poll()) == null) {
                        wait();
                    }
                }

                senderThreadPool.execute(new ChunkSenderThread(chunk));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized private void wakeup() {
        notifyAll();
    }

    public void broadcastChunk(Chunk chunk) {
        for(PeerDefinition pd : PeersList.getPeers()) {
            chunksToSend.add(new ChunkMessage(chunk, pd));
        }
        wakeup();
    }

    public void sendChunk(Chunk chunk, PeerDefinition recipient) {
        chunksToSend.add(new ChunkMessage(chunk, recipient));
        wakeup();
    }

    public void shutdown() {
        stopSending = true;
        this.interrupt();

        if(senderThreadPool != null) {
            senderThreadPool.shutdown();
        }
    }
}
