package ece454p1;

public class QueryMessage extends Message {		
    
	public QueryMessage(PeerDefinition recipient) {
        super(recipient);
    }
    
    public static void broadcast(MessageSender msgSender) {
        for(PeerDefinition pd : PeersList.getPeers()) {
        	msgSender.sendMessage(new QueryMessage(pd));
        }
    }
}

