# File Transfer Application

## Introduction

File Transfer Application between one main.Server and one main.Client with Authentication system built-in.
The problem is to create a file transfer system that utilizes Socket Programming, Connection Management, Reliable Communication, and security protocol that utilizes SHA-1. It allows 2 individuals (1 server and 1 client) on different computers to communicate via network. This project supports Windows, Mac, and Linux.

We use Java to build this program. We create 2 different projects/modules, a main.Client.java and a main.Server.java, both lie within the FileTransferApplication project. The server sets up the sockets and wait for connection. The client sets up its socket and connect to the server. This program also records the start and end time of the session.

There are several commands that allows the client to communicate to the server. The server does not initiate any message. It waits for the commands from the client and sends responses. The client can request server to view all the files the server contains, to download files from the server, or to upload files from local host to store in the server. All messages transported over the network shall be secured with respect to Authentication, Confidentiality, and Integrity.
Build on top of my [Chatroom](https://github.com/baonguyen96/Chatroom) project.


## Execution

### To run program via IDE

1. Open main.Server and main.Client projects on separated windows.
2. Then individually click "Run" to run each code in parallel.

*NOTE*: May have to configure classpath to successfully compile and run (for Eclipse)

### To run program in the command line environment

1. Navigate to [Shell](./Shell) directory
2. Run `./build.windows.ps1`
3. Run `./start-server.ps1`
4. Run `./start-client.ps1`

*NOTE*: `attack.FakeClient` and `attack.FakeServer` are for testing and demo only.
