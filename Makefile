JCC = javac
JCCFLAGS = -J-Xms4096m -J-Xmx4096m -g
JFLAGS = -Xmx4096m -cp src/
SRCDIR=src/ece454
RUNARGS=0 8000
LIBS=lib/*

.SUFFIXES: .java .class

.java.class:
	$(JCC) $(JCCFLAGS) -classpath $(LIBS) -sourcepath $(SRCDIR) $(SRCDIR)/*.java $(SRCDIR)/*/*.java

CLASSES = \
	$(SRCDIR)/PeerDefinition.java \
	$(SRCDIR)/ISocketHandlerThreadFactory.java \
	$(SRCDIR)/messages/ChunkMessage.java \
	$(SRCDIR)/messages/DeleteMessage.java \
	$(SRCDIR)/messages/Message.java \
	$(SRCDIR)/messages/NodeListMessage.java \
	$(SRCDIR)/messages/PullMessage.java \
	$(SRCDIR)/messages/QueryMessage.java \
	$(SRCDIR)/MessageSender.java \
	$(SRCDIR)/NodeAddressServer.java \
	$(SRCDIR)/PeerFileListInfo.java \
	$(SRCDIR)/Peer.java \
	$(SRCDIR)/PeersList.java \
	$(SRCDIR)/SocketAcceptor.java \
	$(SRCDIR)/SocketHandlerThread.java \
	$(SRCDIR)/storage/Chunk.java \
	$(SRCDIR)/storage/DistributedFile.java \
	$(SRCDIR)/storage/IncompleteFileMetadata.java \
	$(SRCDIR)/util/Config.java \
	$(SRCDIR)/util/FileUtils.java \
	$(SRCDIR)/util/ReturnCodes.java \
	$(SRCDIR)/util/SocketUtils.java \
	$(SRCDIR)/ece454.java

default: classes

classes: $(CLASSES:.java=.class)

run:
	java $(JFLAGS) -classpath src/:$(LIBS) ece454/ece454 ${RUNARGS}

clean:
	$(RM) $(SRCDIR)/*.class
	$(RM) $(SRCDIR)/*/*.class