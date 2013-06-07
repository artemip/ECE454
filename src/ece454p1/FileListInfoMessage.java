package ece454p1;

public class FileListInfoMessage extends Message{
	private PeerFileListInfo fileListInfo;
	
    public FileListInfoMessage(PeerDefinition recipient, PeerFileListInfo fListInfo) {
        super(recipient);
        this.fileListInfo = fListInfo;
    }
    
    public PeerFileListInfo getPeerFileListInfo(){
    	return this.fileListInfo;
    }
    
    public static void sendBack(MessageSender sender, PeerDefinition source, PeerFileListInfo fListInfo){
    	//shouldn't broadcast to all peers, only target querying peer
        sender.sendMessage(new FileListInfoMessage(source, fListInfo));
    }
}
