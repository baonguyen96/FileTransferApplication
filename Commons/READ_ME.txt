This is Commons module.
It contains all shared components between Client and Server, such as:
    Cryptor: encryption/decryption
    InvalidMessageException: custom exception to deal with  wrong messages
    Message: add/extract message sequence to/from the actual message
    Peer: superclass of Client and Server that provides common functionality between the 2
    Printable: interface for printing out the raw messages sent over network (for debugging)
    Resynchronizable: interface for changing the behavior of the simulated intruders
