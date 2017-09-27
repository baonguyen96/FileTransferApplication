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
    private static final String BIG_DIV = "\n======================================================\n";
    private static final String SMALL_DIV = "\n---------------------\n";

    public static void main(String[] args) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date;
        boolean connectSuccess = false;
        boolean stopCommunication = false;
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
            System.out.println("\nError: cannot find file.");
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
     * @return true if valid command, false if not
     */
    private static boolean isValidCommand(String clientCommand) {
        String[] commandTokens = clientCommand.split("\\s+\\|\\s+");
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
     *
     *
     * @return true if stop communication, false if not
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static boolean communicate() throws IOException {
        String receivedCommand = "";
        boolean stopConnectionAfterThisCommunication = false;
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));

        if (receivedInput.hasNextLine()) {
            receivedCommand = receivedInput.nextLine();

            /*
             * stay command can either be:
             *      error on client side and client is trying to fix
             *      internal command for client
             */
            if(isValidCommand(receivedCommand) &&
                    !receivedCommand.equalsIgnoreCase("stay")) {
                System.out.println("[Client]: " + receivedCommand);
            }
        }
        else {
            System.out.println(">> Client is offline.");
            return true;
        }

        // process to send message
        if (isValidCommand(receivedCommand)) {
            String[] commandTokens = receivedCommand.split("\\s+\\|\\s+");

            if(commandTokens[0].equalsIgnoreCase("quit")) {
                stopConnectionAfterThisCommunication = true;
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

            }

        }

        // call to disconnect when the client initiates bye
        return stopConnectionAfterThisCommunication;
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
        }
        else if (files.length == 0) {
            messageToSend.append("Empty files directory.");

            System.out.println(">> " + messageToSend.toString());
            printWriter.println(messageToSend.toString());
            printWriter.flush();
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
     * download [file]
     *
     * let uer download files from the library
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
            printWriter.printf("Sending \"%s\" ...\n", fileToSendName);
            System.out.printf(">> Sending \"%s\" ...\n", fileToSendName);

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
     * upload [filePath] command
     *
     * receive the file from user and save it
     *
     * @param commandTokens: parts of client's command
     * @throws IOException
     */
    private static void clientUpload(String[] commandTokens) throws IOException {
        String[] uploadedFilePathComponents = commandTokens[1].split("\\\\");
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
        absolutePath = absolutePath.replace("\\src", "");
        absolutePath = absolutePath.replace("\\Server\\Server", "\\Server");
        directory = new File(absolutePath);
    }

}
