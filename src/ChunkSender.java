package ece454p1;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

public class ChunkSender extends Thread {

    private class ChunkMessage {
        private Chunk chunk;
        private PeerDefinition recipient;

        public ChunkMessage(Chunk chunk, PeerDefinition recipient) {
            this.chunk = chunk;
            this.recipient = recipient;
        }

        public ChunkMessage(Chunk chunk) {
            this.chunk = chunk;
        }

        public Chunk getChunk() {
            return chunk;
        }

        public PeerDefinition getRecipient() {
            return recipient;
        }

        public boolean isBroadcast() {
            return recipient == null;
        }
    }

    private class ChunkSenderThread implements Runnable {

        private ChunkMessage chunkMessage;

        public ChunkSenderThread(ChunkMessage chunkMessage) {
            this.chunkMessage = chunkMessage;
        }
        @Override
        public void run() {

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
        chunksToSend.add(new ChunkMessage(chunk));
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
