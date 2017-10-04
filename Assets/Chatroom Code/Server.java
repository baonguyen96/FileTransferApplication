import java.io.*;                   // for IO operations
import java.net.*;                  // for networking apps
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        String messageReceived = "";    // message from the server
        String messageToSend = "";      // message to send out
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date;
        boolean connectSuccess = false;

        System.out.println("\n==================================================\n");
        System.out.println("Waiting for connection...");

        try {
            /*
             * create new server socket and bind to port 1111 (same as client)
             * wait 30 seconds for client to connect
             * without connection within this time frame,
             * then close the socket automatically
             */
            ServerSocket serverSocket = new ServerSocket(1111);
            serverSocket.setSoTimeout(30000);

            // accept a client socket
            Socket clientSocket = serverSocket.accept();

            // reading input from the keyboard
            Scanner keyboardInput = new Scanner(System.in);

            // send data to client from an existing outputStream of clientSocket
            OutputStream outputStream = clientSocket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);

            // receive data
            InputStream inputStream = clientSocket.getInputStream();
            Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));

            // set current date time
            date = new Date();
            System.out.println("Connection established at " + dateFormat.format(date) + "\n");
            connectSuccess = true;

            // stay connected until the client disconnects
            while (true) {
                /*
                 * if receive anything from the client, then print it out
                 * else, the client is offline -> indicate error to the screen
                 */
                if (receivedInput.hasNextLine()) {
                    messageReceived = receivedInput.nextLine();
                    System.out.println("[Client]: " + messageReceived);
                }
                else {
                    System.out.println("\nClient is offline.");
                    break;
                }

                // call to disconnect when the server initiates bye
                if (disconnect(messageToSend, messageReceived, clientSocket, serverSocket)) {
                    break;
                }

                // prompt server to type
                System.out.print("[You]:    ");

                // read input from the keyboard
                messageToSend = keyboardInput.nextLine();

                // send message to the server
                printWriter.println(messageToSend);

                // flush the printWriter buffer
                printWriter.flush();

                // call to disconnect when the client initiates bye
                if (disconnect(messageToSend, messageReceived, clientSocket, serverSocket)) {
                    break;
                }
            }
        }
        catch (SocketTimeoutException e) {
            // wait too long for a client to connect
            System.out.println("\nError: waited too long for connection.");
        }
        catch (IOException e) {
            // error connection
            System.out.println("\nError: failed to create socket.");
        }
        finally {
            // prompt to indicate end of session
            date = new Date();
            System.out.printf("\nConnection %s at %s\n",
                    connectSuccess ? "ended" : "failed",
                    dateFormat.format(date));
            System.out.println("\n==================================================\n");
        }
    }


    /***
     * method: disconnect
     * when both send and receive messages say "bye"
     * (confirm from both sides), end connection
     *
     * @param sendMsg: message to send out
     * @param recvMsg: message received
     * @param client: client socket
     * @param server: server socket
     * @return true if close all sockets, false if not
     * @throws IOException: null sockets
     */
    public static boolean disconnect(String sendMsg, String recvMsg, Socket client,
                                     ServerSocket server) throws IOException {
        if(sendMsg != null && recvMsg != null &&
                sendMsg.contains("bye") &&
                recvMsg.contains("bye")) {
            client.close();
            server.close();
            return true;
        }
        return false;
    }

}
