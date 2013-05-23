package ece454p1;

/**
 * Peer and Status are the classes we really care about Peers is a container;
 * feel free to do a different container
 */
public class Peer {
	public int insert(String filename) {
        //Add File of name 'filename' to the local peer
        //If the file DNE, return an error code
        //If it does exist, then copy and store it alonf with other files in this peer (and return 0)
        //Then, push out the file to all other peers (split into chunks, send, and distribute among all peers (somehow))
        return ReturnCodes.OK;
    }

	public int query(Status status) {
        //Populate 'status' with information regarding a file
        //PAREMETERS:
        //1) Fraction of file that is available locally
        //2) Fraction of file available in the system
        //3) Least replication level
        //4) Weighted least-replication level

        return ReturnCodes.OK;
    }

	/*
	 * Note that we should have the peer list, so it is not needed as a
	 * parameter
	 */
	public int join() {
        //Sync with all peers: push local files, pull external files (that it does not already have)
        return ReturnCodes.OK;
    }

	public int leave() {
        //Close all sockets
        //Inform all peers of absence
        //Preferred: push out rare file chunks before leaving
        return ReturnCodes.OK;
    }


    //TODO: can we achieve this without a programming API? Can we use socket message-passing exclusively?
	private enum State {
		CONNECTED, DISCONNECTED
	};

	private State currentState;
	private Peers peers;

}
