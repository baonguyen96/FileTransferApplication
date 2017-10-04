import java.io.*;                       // for IO operations
import java.net.*;                      // for networking apps
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String messageReceived = "";    // message from the server
        String messageToSend = "";      // message to send out
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date;
        boolean connectSuccess = false;

        System.out.println("\n==================================================\n");
        System.out.println("Setting up the connection...");

        try {
            // create a socket for client with the local host and port 1111
            Socket clientSocket = new Socket("localhost", 1111);

            // reading input from the keyboard
            Scanner keyboardInput = new Scanner(System.in);

            // send data to client from an existing outputStream of clientSocket
            OutputStream outputStream = clientSocket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);

            // receive data from the server
            InputStream inputStream = clientSocket.getInputStream();
            Scanner serverInput = new Scanner(new InputStreamReader(inputStream));

            // current date time of the connection
            date = new Date();
            System.out.println("Connection established at " + dateFormat.format(date) + "\n");
            connectSuccess = true;

            // stay connected
            while (true) {
                // prompt client to type
                System.out.print("[You]:    ");

                // read input from the keyboard
                messageToSend = keyboardInput.nextLine();

                // send message to the server
                printWriter.println(messageToSend);

                // flush the printWriter buffer
                printWriter.flush();

                // call to disconnect when the server initiates bye
                if (disconnect(messageToSend, messageReceived, clientSocket)) {
                    break;
                }

                /*
                 * if receive anything from the server, then print it out
                 * else, the server is offline -> indicate error to the screen
                 */
                if (serverInput.hasNextLine()) {
                    messageReceived = serverInput.nextLine();
                    System.out.println("[Server]: " + messageReceived);

                    // call to disconnect when the client initiates bye
                    if (disconnect(messageToSend, messageReceived, clientSocket)) {
                        break;
                    }
                }
                else {
                    System.out.println("\nServer is offline.");
                    break;
                }
            }
        }
        catch(IOException e) {
            // cannot connect to the server
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
     * @return true if close all sockets, false if not
     * @throws IOException: null sockets
     */
    public static boolean disconnect(String sendMsg, String recvMsg, Socket client)
            throws IOException {
        if(sendMsg != null && recvMsg != null &&
                sendMsg.contains("bye") &&
                recvMsg.contains("bye")) {
            client.close();
            return true;
        }
        return false;
    }
}
