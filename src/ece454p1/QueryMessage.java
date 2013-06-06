package ece454p1;

public class QueryMessage extends Message {
    private Status status;

    public QueryMessage(PeerDefinition recipient, Status status) {
        super(recipient);
        this.status = status;
    }

    public static void broadcast(MessageSender sender) {
        for(PeerDefinition pd : PeersList.getPeers()) {
	    //            sender.sendMessage(new QueryMessage(pd));
        }
    }

}

