This is Commons module.
It contains all shared components between Client and Server, such as:
    Message: provides any message-related tasks (such as add/extract sequence number, encrypt/decrypt)
    InvalidMessageException: custom exception to deal with  wrong messages
    Peer: superclass of Client and Server that provides common functionality between the 2