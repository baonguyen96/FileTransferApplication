This is Commons module.
It contains all shared components between Client and Server, such as:
    message.translation.Cryptor: encryption/decryption
    utils.InvalidMessageException: custom exception to deal with  wrong messages
    message.translation.Message: add/extract message sequence to/from the actual message
    peer.Peer: superclass of Client and Server that provides common functionality between the 2
    utils.Printable: interface for printing out the raw messages sent over network (for debugging)
    utils.Resynchronizable: interface for changing the behavior of the simulated intruders
