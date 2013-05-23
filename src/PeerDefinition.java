package ece454p1;

public class PeerDefinition {
    private String ipAddress;
    private int port;

    public PeerDefinition(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIPAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getFullAddress() {
        return ipAddress + port;
    }

    public static PeerDefinition fromString(String peerDefinitionString) throws MalformedPeerDefinitionException {
        String[] splitLine = peerDefinitionString.split(" ");

        if(splitLine.length != 2)
            throw new MalformedPeerDefinitionException();

        String ipAddress = splitLine[0];
        int port = Integer.getInteger(splitLine[1]);

        return new PeerDefinition(ipAddress, port);
    }

    public static class MalformedPeerDefinitionException extends Exception {}
}
