ECE454-P1
=========

TODO:
- Makefile
- Run each peer individually
- Receive/Send startup messages from/to other peers and start up TCP socket connections
- Monitor directories for added files (Maybe? See: http://docs.oracle.com/javase/tutorial/essential/io/notification.html)
- Add files programatically (via insert())
  - Copy to local storage directory
  - Push out to all other peers via TCP socket
    - Chunk data, and maintain some global metadata about chunks and file statuses

...
