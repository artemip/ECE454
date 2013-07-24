package ece454;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeersList {
    private static List<PeerDefinition> peers;

    public static void initialize(PeerDefinition[] peersList) {
        for(PeerDefinition p : peersList) {
            peers.add(p);
        }
    }

    public static void addPeer(PeerDefinition definition) {
        peers.add(definition);
    }

    public static PeerDefinition getPeerById(int peerId) {
        for(PeerDefinition pd : peers) {
            if(pd.getId() == peerId)
                return pd;
        }

        return null;
    }

    public static List<PeerDefinition> getPeers() {
        return peers;
    }
}
