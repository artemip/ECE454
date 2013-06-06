package ece454p1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeersList {
    private static List<PeerDefinition> peers;

	/**
	 * The peersFile is the name of a file that contains 
	 * a list of the peers. Its format is as follows: 
	 * in plaintext there are up to maxPeers lines, where
	 * each line is of the form: <IP address> <port number> 
	 * This file should be available on every machine 
	 * on which a peer is started, though you should
	 * exit gracefully if it is absent or incorrectly formatted. 
	 * After execution of this method, the peers should be present.
	 * 
	 * @param peersFile
	 * @return
	 */
	public static int initialize(String peersFile, int peerId) {
        peers = new ArrayList<PeerDefinition>();

        BufferedReader fileReader;
        String line;

        try {
            if(!(new File(peersFile).exists()))
                return ReturnCodes.PEERS_FILE_NOT_FOUND_ERR;

            fileReader = new BufferedReader(new FileReader(peersFile));

            int index = 0;

            while((line = fileReader.readLine()) != null) {
                // Only add *other* peers
                if(index++ != peerId)
                    peers.add(PeerDefinition.fromString(line));
            }

            fileReader.close();
        } catch (IOException e) {
            return ReturnCodes.PEERS_FILE_ERR;
        } catch (PeerDefinition.MalformedPeerDefinitionException e) {
            return ReturnCodes.PEERS_FILE_ERR;
        }

        return ReturnCodes.OK;
    }

    public static PeerDefinition getPeerByAddress(String ipAddress, int port) {
        String fullAddress = ipAddress + ":" + port;

        for(PeerDefinition pd : peers) {
            if(pd.getFullAddress() == fullAddress)
                return pd;
        }

        return null;
    }

    public static List<PeerDefinition> getPeers() {
        return peers;
    }
}
