# File Transfer Application

## Introduction

File Transfer Application between one Server and one Client with Authentication system built-in.
The problem is to create a file transfer system that utilizes Socket Programming, Connection Management, Reliable Communication, and security protocol that utilizes SHA-1. It allows 2 individuals (1 server and 1 client) on different computers to communicate via network. This project supports Windows, Mac, and Linux.
We use Java to build this program. We create 2 different projects/modules, a Client.java and a Server.java, both lie within the FileTransferApplication project. The server sets up the sockets and wait for connection. The client sets up its socket and connect to the server. This program also records the start and end time of the session. 
There are several commands that allows the client to communicate to the server. The server does not initiate any message. It waits for the commands from the client and sends responses. The client can request server to view all the files the server contains, to download files from the server, or to upload files from local host to store in the server. All messages transported over the network shall be secured with respect to Authentication, Confidentiality, and Integrity.
Build on top of my [Chatroom](https://github.com/baonguyen96/Chatroom) project.

## Configuration

All required files must be in this structure:
<pre>
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
</pre>

All the files that client uploads reside in Server/FilesDirectory folder.

All the files that client downloads from the server reside in Client/FilesDirectory folder.

## Execution

### To run program via IDE
    
1. Open Server and Client projects on separated windows.
2. Then individually click "Run" to run each code in parallel.

*NOTE*: May have to configure classpath to successfully compile and run (for Eclipse)

### To run program in the command line environment

*For MAC and Linux:*

1. Open terminal and navigate to [Common/src](./Common/src/) folder
2. Type: `javac *.java`
3. Navigate to [Server/src](./Server/src/) folder
4. Type: `javac -cp ..\..\Commons\src *.java`
5. Type: `java -cp ..\..\Commons\src:. Server`
6. Open another terminal and navigate to [Client/src](./Client/src/) folder
7. Type: `javac -cp ..\..\Commons\src *.java`
8. Type: `java -cp ..\..\Commons\src:. Client`

*For Windows:*

1. Open CMD (NOT PowerShell) and navigate to [Common/src](./Common/src/) folder
2. Type: `javac *.java`
3. Navigate to [Server/src](./Server/src/) folder
4. Type: `javac -cp ..\..\Commons\src *.java`
5. Type: `java -cp ..\..\Commons\src;. Server`
6. Open another CMD window and navigate to [Client/src](./Client/src/) folder
7. Type: `javac -cp ..\..\Commons\src *.java`
8. Type: `java -cp ..\..\Commons\src;. Client`

*NOTE*: `FakeClient` and `FakeServer` are for testing and demo only.
