package ece454p1;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Peers is a dumb container to hold the peers; the number of peers is fixed,
 * but needs to be set up when a peer starts up; feel free to use some other
 * container class, but that class must have a method that allows it to read the
 * peersFile, since otherwise you have no way of having a calling entity tell
 * your code what the peers are in the system.
 **/
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
	public static int initialize(String peersFile) {
        peers = new ArrayList<PeerDefinition>();

        BufferedReader fileReader;
        String line;

        try {
            fileReader = new BufferedReader(new FileReader(peersFile));

            while((line = fileReader.readLine()) != null) {
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

    public static List<PeerDefinition> getPeers() {
        return peers;
    }
}
