Authors:
Bao Nguyen - BCN140030
Mofan Li - MXL162930

Program coded using Java in IntelliJ and Eclipse.

1. CONFIGURATION:
Before running the program, user has to make sure the client and server are on the same network. 
If they are on different networks, the server machine has to setup port forwarding before it can 
operate with a remote client. Also, if there is an error saying “Socket corrupted” when the Client 
tries to connect to the Server (and the Server is running), then it is a Firewall configuration problem.
Please check Firewall to enable communication on your current type of network.
For the project to be successfully compiled and run, its structure must be preserved. 
The core modules structure by default - and must not be changed - is ([] denotes directory):

[] Client
|----- [] FilesDirectory
|----- [] src
        |----- CAPublicKey.txt
        |----- Client.java
        |----- FakeClient.java
[] Commons
|----- [] src
        |----- message.translation.Cryptor.java
        |----- utils.InvalidMessageException.java
        |----- message.translation.Message.java
        |----- peer.Peer.java
        |----- utils.Printable.java
        |----- utils.Resynchronizable.java
[] Server
|----- [] FilesDirectory
|----- [] src
        |----- CA-Certificate.crt
        |----- CAPublicKey.txt
        |----- FakeServer.java
        |----- PrivateKey.txt
        |----- PublicKey.txt
        |----- Server.java

All the files that client uploads reside in Server/FilesDirectory folder.
All the files that client downloads from the server reside in Client/FilesDirectory folder.

2. EXECUTION:
There are several ways to run the system.
a. To run program via IDE:
    Open Server and Client projects on separated windows.
    Then individually click "Run" to run each code in parallel.
    (May have to configure classpath to successfully compile and run)
b. To run program in the command line environment:
    For MAC and Linux:
        Open terminal and navigate to Common/src folder
        Type: javac *.java
        Navigate to Server/src folder
        Type: javac -cp ..\..\Commons\src *.java
        Type: java -cp ..\..\Commons\src:. Server
        Open another terminal and navigate to Client/src folder
        Type: javac -cp ..\..\Commons\src *.java
        Type: java -cp ..\..\Commons\src:. Client
    For Windows:
        Open CMD (NOT PowerShell) and navigate to Common/src folder
        Type: javac *.java
        Navigate to Server/src folder
        Type: javac -cp ..\..\Commons\src *.java
        Type: java -cp ..\..\Commons\src;. Server
        Open another CMD window and navigate to Client/src folder
        Type: javac -cp ..\..\Commons\src *.java
        Type: java -cp ..\..\Commons\src;. Client

NOTE: FakeClient and FakeServer are for testing and demo only.