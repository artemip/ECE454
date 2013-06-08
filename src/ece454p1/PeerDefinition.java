package ece454p1;

import java.io.Serializable;

public class PeerDefinition implements Serializable {
    private String ipAddress;
    private int port;
    private int id;

    public PeerDefinition(String ipAddress, int port, int id) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.id = id;
    }

    public String getIPAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getFullAddress() {
        return ipAddress + ":" + port;
    }

    public int getId() {
        return this.id;
    }

    public static PeerDefinition fromString(String peerDefinitionString, int id) throws MalformedPeerDefinitionException {
        String[] splitLine = peerDefinitionString.split(" ");

        if(splitLine.length != 2)
            throw new MalformedPeerDefinitionException();

        String ipAddress = splitLine[0];
        int port = Integer.parseInt(splitLine[1]);

        return new PeerDefinition(ipAddress, port, id);
    }

    public static class MalformedPeerDefinitionException extends Exception {}
}
