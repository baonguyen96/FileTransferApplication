This version is for encapsulating encryption and decryption into a separated
Message class (inside of its own module) for less repeated codes.

------------------------------------------------------------------------------------
------------------------------------------------------------------------------------

Program coded using Java in IntelliJ and Eclipse.

There are several ways to run the system: via IDE or via Command Line environment.
    1. To run program via IDE:
	    	Open Server and Client projects on separated windows.
	    	Then individually click "Run" to run each code in parallel.
	    	(May have to configure classpath to successfully compile and run)
    2. To run program in the command line environment:
	    	Open terminal and navigate to Server/src folder
	    	Type: javac -cp ..\..\Commons\src Server.java
	    	Type: java -cp ..\..\Commons\src;. Server
			Open another terminal and navigate to Client/src folder
	    	Type: javac -cp ..\..\Commons\src Client.java
	    	Type: java -cp ..\..\Commons\src;. Client
    3. To run program as jar files: 
    		To be added later.


------------------------------------------------------------------------------------
------------------------------------------------------------------------------------


NOTE 1: Encryption key is symmetric key and is changing over each session (1 time pad)
NOTE 2: This implementation is only for authentication, which includes:
            exchange encryption key E and
            update E automatically throughout different sessions and
            verify if same client throughout different sessions.
        It does not include integrity checking yet (signature key S).
            You can implement that part.

Protocol requires synchronization between Client and Server to work.

------------------------------------------------------------------------------------

To validate same client over multiple sessions:

Client:
    When the client is created, its creation time is its ID.
    Create random initial master key R
    Create encryption key E = R + 1
    Create signature S = R + 2
    In each session from 1 -> n, update:
        E = E + 2
        S = S + 2

    For authentication:
        Session 0:
            Encrypt R using Server's public key to get R'
            Encrypt ID using E to get ID'
            Send R' and ID'
        Session 1 -> n:
            Encrypt ID using new E to get ID'
            Send ID'


Server:
    Create encryption key E, signature key S (for client)
    In each session from 1 -> n, update:
            E = E + 2
            S = S + 2

    For authentication:
        Session 0:
            Receive R' and ID'
            Decrypt R' using its private key to get back R
            E = R + 1
            Decrypt ID' using E to get client's ID and store
        Session 1 -> n:
            Decrypt ID' using new E to get client's ID
            Compare with stored ID
            If same -> pass
            Else -> fail

------------------------------------------------------------------------------------

To prevent replay attack, append sequence number in front of the message, encrypt, then send.
Format: [sn | id | msg]k
where 	sn = sequence number
		id = client's id
		msg = message in plaintext
		k = session key to encrypt

But what to do when out-of-sync?
    Don't have enough time, therefore just end the connection for client if out of sync
    For server, can't because the fake client will of course be out of sync,
    but the real client isn't -> end server will affect real client