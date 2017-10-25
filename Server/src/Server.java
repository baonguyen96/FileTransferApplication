/***
 * Server
 *
 * Bao Nguyen
 * CS 6349.001
 *
 * Possible client commands:
 *      quit:            end session
 *      list:            list all files in the FilesDirectory of the server
 *      download | file: download the existing "file" in the FilesDirectory of the server
 *      upload | file:   upload the "file" to the FilesDirectory of the server
 *      stay:            keep session alive (command error on client side)
 */

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Server {
    private static Socket clientSocket = null;
    private static File directory = null;
    private static final String PRIVATE_KEY = getKey("PrivateKey.txt");
    private static final String PUBLIC_KEY = getKey("PublicKey.txt");
    private static boolean isBusy = false;
    private static String clientIpAddress = null;
    private static String clientId = null;


    public static void main(String[] args) {
        // error with keys then stop
        if(PRIVATE_KEY == null || PUBLIC_KEY == null) {
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = null;
        boolean stopCommunication = false;
        final String BIG_DIV = "\n======================================================\n";
        final String SMALL_DIV = "\n---------------------\n";

        setDirectory();

        try {
            System.out.println(BIG_DIV);
            System.out.println("IP address: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Waiting for connection...");

            ServerSocket serverSocket = new ServerSocket(1111);
            serverSocket.setSoTimeout(120000);


            /*
             * stay connected until the client disconnects
             * after the first successful connection, remember the IP address of the client
             * so that in the subsequent reconnection, does not accept any host that has different address
             */
            while (!stopCommunication) {
                clientSocket = serverSocket.accept();
                
                // make sure to talk to the same client over several sessions
                if(!authenticate()) {
                    clientSocket.close();
                    continue;
                }

                /*
                 * if successfully connect for the first time:
                 *      set initial connection start time
                 *      from now is busy
                 */
                if (!isBusy) {
                    date = new Date();
                    System.out.println("Connection established at " + dateFormat.format(date));
                    System.out.println(SMALL_DIV);
                    isBusy = true;
                }

                stopCommunication = communicate();
                clientSocket.close();

            }
            serverSocket.close();

        }
        catch (UnknownHostException e) {
            System.out.println("\nError: unknown IP address.");
        }
        catch (SocketTimeoutException e) {
            System.out.println("\nError: waited too long for connection.");
        }
        catch (FileNotFoundException e) {
            System.out.println("\nError: cannot create or find file.");
        }
        catch (IOException e) {
            System.out.println("\nError: sockets corrupted.");
        }
        finally {
            System.out.println(SMALL_DIV);
            date = new Date();
            System.out.printf("Connection %s at %s\n",
                    isBusy ? "ended" : "failed",
                    dateFormat.format(date));
            System.out.println(BIG_DIV);
        }
    }


    /***
     * method: isValidCommand
     *
     * validate the command
     *
     * @param clientCommand: client's command
     * @param delimiter: split signature
     *
     * @return true if valid command, false if not
     */
    private static boolean isValidCommand(String clientCommand, String delimiter) {
        String[] commandTokens = clientCommand.split(delimiter);
        boolean isQuit = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("quit");
        boolean isList = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("list");
        boolean isStay = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("stay");
        boolean isDownload = commandTokens.length == 2 &&
                commandTokens[0].equalsIgnoreCase("download");
        boolean isUpload = commandTokens.length == 2 &&
                commandTokens[0].equalsIgnoreCase("upload");

        return isQuit || isList || isStay || isDownload || isUpload;
    }


    /***
     * method: communicate
     *
     * communication session between server and client
     *
     * @return true if stop communication, false if not
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static boolean communicate() throws IOException {
        String receivedCommand = "";
        final boolean STOP_CONNECTION_AFTER_THIS = true;
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));
        final String DELIMITER = "\\s+\\|\\s+";

        // offline
        if(!receivedInput.hasNextLine()) {
            System.out.println(">> Client is offline.");
            return STOP_CONNECTION_AFTER_THIS;
        }

        // invalid command or stay command
        receivedCommand = receivedInput.nextLine();
        if(!isValidCommand(receivedCommand, DELIMITER) ||
                receivedCommand.equalsIgnoreCase("stay")) {
            return !STOP_CONNECTION_AFTER_THIS;
        }

        // valid command
        System.out.println("[Client]: " + receivedCommand);
        String[] commandTokens = receivedCommand.split(DELIMITER);

        if(commandTokens[0].equalsIgnoreCase("quit")) {
            return STOP_CONNECTION_AFTER_THIS;
        }
        else if(commandTokens[0].equalsIgnoreCase("list")) {
            list();
        }
        else if(commandTokens[0].equalsIgnoreCase("download")) {
            clientDownload(commandTokens);
        }
        else if(commandTokens[0].equalsIgnoreCase("upload")){
            clientUpload(commandTokens);
        }

        return !STOP_CONNECTION_AFTER_THIS;
    }


    /***
     * method: list
     *
     * list command
     *
     * send to client a list of all files the server contains
     *
     * @throws IOException
     */
    private static void list() throws IOException {
        File[] files = directory.listFiles();
        StringBuilder messageToSend = new StringBuilder();
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);

        if(files == null) {
            messageToSend.append("Error: cannot find files directory.");

            System.out.println(">> " + messageToSend.toString());
            printWriter.println(messageToSend.toString());
            printWriter.flush();
            printWriter.close();
        }
        else if (files.length == 0) {
            messageToSend.append("Empty files directory.");

            System.out.println(">> " + messageToSend.toString());
            printWriter.println(messageToSend.toString());
            printWriter.flush();
            printWriter.close();
        }
        else {
            System.out.println(">> Sending list of files...");

            for (int i = 0; i < files.length; i++) {
                messageToSend.append(files[i].getName());

                if (i != files.length - 1) {
                    messageToSend.append(", ");
                }
            }

            printWriter.println(messageToSend.toString());
            printWriter.flush();
            printWriter.close();
            System.out.println(">> Complete sending list of files");
        }
        System.out.println();
    }


    /***
     * method: clientDownload
     *
     * download | file
     *
     * let client download files from the library
     *
     * @param commandTokens: parts of client's command
     * @throws IOException
     */
    private static void clientDownload(String[] commandTokens) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        String fileToSendName = commandTokens[1];

        try {
            File fileToSend = new File(directory.getAbsolutePath()
                    + "/" + fileToSendName);
            byte[] byteArray = new byte[(int) fileToSend.length()];
            fileInputStream = new FileInputStream(fileToSend);
            bufferedInputStream = new BufferedInputStream(fileInputStream);

            // confirmation message
            String confirmationMessage = String.format("Sending \"%s\" ...", fileToSendName);
            printWriter.println(confirmationMessage);
            System.out.println(">> " + confirmationMessage);

            // file transfer
            bufferedInputStream.read(byteArray, 0, byteArray.length);
            bufferedOutputStream.write(byteArray, 0, byteArray.length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

            System.out.printf(">> Complete sending \"%s\"\n", fileToSendName);
        }
        catch (FileNotFoundException e) {
            String error = "Error: requested file does not exist.";
            System.out.println("\n[You]:    " + error);
            printWriter.println(error);
            printWriter.flush();
            printWriter.close();
        }
        finally {
            System.out.println();
        }

    }


    /***
     * method: clientUpload
     *
     * upload | filePath command
     *
     * receive the file from client and save it
     *
     * @param commandTokens: parts of client's command
     * @throws IOException
     */
    private static void clientUpload(String[] commandTokens) throws IOException {
        String filePath = commandTokens[1].replace("\\", "/");
        String[] uploadedFilePathComponents = filePath.split("/");
        String uploadedFileName = uploadedFilePathComponents[uploadedFilePathComponents.length - 1];
        File receivingFile = new File(directory.getAbsolutePath() +
                "/" + uploadedFileName);
        InputStream inputStream = clientSocket.getInputStream();
        byte[] byteBlock = new byte[1];
        FileOutputStream fileOutputStream = new FileOutputStream(receivingFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        int byteRead = inputStream.read(byteBlock, 0, byteBlock.length);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        System.out.printf(">> Receiving \"%s\" ...\n", uploadedFileName);

        while(byteRead >= 0) {
            byteArrayOutputStream.write(byteBlock);
            byteRead = inputStream.read(byteBlock);
        }

        bufferedOutputStream.write(byteArrayOutputStream.toByteArray());
        bufferedOutputStream.flush();
        bufferedOutputStream.close();
        inputStream.close();

        System.out.printf(">> Complete saving \"%s\"\n\n", uploadedFileName);

    }


    /***
     * method: setDirectory
     *
     * set the Files Directory to store all files
     * remove the "\src" in the path when run from the command line environment
     */
    private static void setDirectory() {
        directory = new File("Server/FilesDirectory");
        String absolutePath = directory.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
        absolutePath = absolutePath.replace("/src", "");
        absolutePath = absolutePath.replace("/Server/Server", "/Server");
        directory = new File(absolutePath);
    }


    /***
     * method: authenticate
     *
     * client authenticates server with keys
     * prevent multiple clients to connect simultaneously
     *
     * @throws IOException
     */
    private static boolean authenticate() throws IOException {
        boolean authenticateSuccess = false;

        /*
         * PROTOTYPE:
         *
         * if not busy (waiting for first connection):
         *      key exchange
         *      receive encrypted client id
         *      decrypt client id and store
         * if busy:
         *      receive encrypted client id
         *      decrypt and check
         *      if not the same then reject
         *
         * NOTE: session key will be increment after each session
         */

        // key exchange
        // encrypt/decrypt

        // just to check the id
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));
        String clientMessage = receivedInput.nextLine();

        if(clientId == null) {
            clientId = clientMessage;
            clientIpAddress = clientSocket.getInetAddress().getHostAddress();
            authenticateSuccess = true;
        }
        else {
            authenticateSuccess = clientMessage.equals(clientId) &&
                    clientSocket.getInetAddress().getHostAddress().equals(clientIpAddress);
        }

        printWriter.println(authenticateSuccess ? "ok" : "busy");
        printWriter.flush();

        return authenticateSuccess;
    }


    /***
     * method: getKey
     *
     * read the key from the local file and return as string
     *
     * @param keyFileName: local file that contains the key
     * @return key as string
     */
    private static String getKey(String keyFileName) {
        File file = new File(keyFileName);
        if(!file.exists()) {
            file = new File("Server/src/" + keyFileName);
        }
        StringBuilder key = new StringBuilder();

        try {
            Scanner scanner = new Scanner(file);
            while(scanner.hasNextLine()) {
                key.append(scanner.nextLine());
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("Error: Cannot find " + keyFileName);
            key = null;
        }

        return key == null ? null : key.toString();
    }

}

