package ece454p1;

public class QueryMessage extends Message {		
    
	public QueryMessage(PeerDefinition recipient, int senderId) {
        super(recipient, senderId);
    }
    
    public static void broadcast(MessageSender msgSender, int senderId) {
        for(PeerDefinition pd : PeersList.getPeers()) {
        	msgSender.sendMessage(new QueryMessage(pd, senderId));
        }
    }
}

