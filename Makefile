JCC = javac
JCCFLAGS = -J-Xms4096m -J-Xmx4096m -g
JFLAGS = -Xmx4096m -cp src/
SRCDIR=src/ece454p1

.SUFFIXES: .java .class

.java.class:
	$(JCC) $(JCCFLAGS) -cp $(SRCDIR) $(SRCDIR)/*.java

CLASSES = \
	$(SRCDIR)/PeerDefinition.java \
	$(SRCDIR)/IncompleteFileMetadata.java \
	$(SRCDIR)/Message.java \
	$(SRCDIR)/Chunk.java \
	$(SRCDIR)/ChunkMessage.java \
	$(SRCDIR)/Config.java \
	$(SRCDIR)/DistributedFile.java \
	$(SRCDIR)/ece454p1.java \
	$(SRCDIR)/FileUtils.java \
	$(SRCDIR)/MessageSender.java \
	$(SRCDIR)/Peer.java \
	$(SRCDIR)/PeersList.java \
	$(SRCDIR)/PullMessage.java \
	$(SRCDIR)/QueryMessage.java \
	$(SRCDIR)/ReturnCodes.java \
	$(SRCDIR)/Status.java

default: classes

classes: $(CLASSES:.java=.class)

run:
	java $(JFLAGS) ece454p1/ece454p1 8000 peers.txt

clean:
	$(RM) $(SRCDIR)/*.class