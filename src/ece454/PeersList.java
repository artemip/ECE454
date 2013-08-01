package ece454;

import java.util.ArrayList;
import java.util.List;

public class PeersList {
    private static List<PeerDefinition> peers = new ArrayList<PeerDefinition>();

    public static void initialize(PeerDefinition[] peersList, int localPeerId) {
        peers = new ArrayList<PeerDefinition>();

        for (PeerDefinition p : peersList) {
            if(p.getId() != localPeerId)
                peers.add(p);
        }
    }

    public static void addPeer(PeerDefinition definition) {
        peers.add(definition);
    }

    public static PeerDefinition getPeerById(int peerId) {
        for (PeerDefinition pd : peers) {
            if (pd.getId() == peerId)
                return pd;
        }

        return null;
    }

    public static void removePeer(int peerId) {
        for(int i = 0; i < peers.size(); i++) {
            if(peers.get(i).getId() == peerId) {
                peers.remove(i);
                break;
            }
        }
    }

    public static List<PeerDefinition> getPeers() {
        return peers;
    }
}
