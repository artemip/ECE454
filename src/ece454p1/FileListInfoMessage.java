package ece454p1;

public class FileListInfoMessage extends Message{
	private PeerFileListInfo fileListInfo;
	
    public FileListInfoMessage(PeerDefinition recipient, PeerFileListInfo fListInfo, int senderId) {
        super(recipient, senderId);
        this.fileListInfo = fListInfo;
    }
    
    public PeerFileListInfo getPeerFileListInfo(){
    	return this.fileListInfo;
    }
    
    public static void replyToQuery(MessageSender sender, PeerDefinition source, int senderId, PeerFileListInfo fListInfo){
    	//shouldn't broadcast to all peers, only target querying peer
        sender.sendMessage(new FileListInfoMessage(source, fListInfo, senderId));
    }
}
