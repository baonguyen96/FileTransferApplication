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


    public static void main(String[] args) {
        // error with keys then stop
        if(PRIVATE_KEY == null || PUBLIC_KEY == null) {
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = null;
        boolean connectSuccess = false;
        boolean stopCommunication = false;
        final String BIG_DIV = "\n======================================================\n";
        final String SMALL_DIV = "\n---------------------\n";

        setDirectory();

        try {
            System.out.println(BIG_DIV);
            System.out.println("IP address: " + Inet6Address.getLocalHost().getHostAddress());
            System.out.println("Waiting for connection...");

            /*
             * create new server socket and bind with port 1111 (same as client)
             * wait 2 minutes for client to connect
             * without connection within this time frame,
             * then close the socket automatically
             */
            ServerSocket serverSocket = new ServerSocket(1111);
            serverSocket.setSoTimeout(120000);

            // stay connected until the client disconnects
            while (!stopCommunication) {
                clientSocket = serverSocket.accept();
                serverSocket.setSoTimeout(Integer.MAX_VALUE);

                if(!connectSuccess) {
                    date = new Date();
                    System.out.println("Connection established at " + dateFormat.format(date));
                    System.out.println(SMALL_DIV);
                    connectSuccess = true;
                }

                stopCommunication = communicate();
                clientSocket.close();
            }

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
                    connectSuccess ? "ended" : "failed",
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
        boolean isDownload = commandTokens.length == 2 &&
                commandTokens[0].equalsIgnoreCase("download");
        boolean isUpload = commandTokens.length == 2 &&
                commandTokens[0].equalsIgnoreCase("upload");
        boolean isStay = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("stay");

        return isQuit || isList || isDownload || isUpload || isStay;
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

        if(!receivedInput.hasNextLine()) {
            System.out.println(">> Client is offline.");
            return STOP_CONNECTION_AFTER_THIS;
        }

        receivedCommand = receivedInput.nextLine();

        /*
         * stay command can either be:
         *      error on client side and client is trying to fix
         *      internal command for client
         */
        if(isValidCommand(receivedCommand, DELIMITER) &&
                !receivedCommand.equalsIgnoreCase("stay")) {

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
            else {
                // don't do anything for "stay" command
            }
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
     */
    private static void authenticate() throws IOException {

    }


    private static String getKey(String keyFileName) {
        File file = new File(keyFileName);
        if(!file.exists()) {
            keyFileName = "Server/src/" + keyFileName;
            file = new File(keyFileName);
        }
        StringBuilder key = new StringBuilder();
        boolean getKeySuccess = false;

        try {
            Scanner scanner = new Scanner(file);
            key = new StringBuilder();
            while(scanner.hasNextLine()) {
                key.append(scanner.nextLine());
            }
            getKeySuccess = true;
        }
        catch (FileNotFoundException e) {
            System.out.println("Error: Cannot find " + keyFileName);
            e.printStackTrace();
        }

        return getKeySuccess ? key.toString() : null;
    }
}

