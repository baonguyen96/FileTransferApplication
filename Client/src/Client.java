import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client {
    private static Socket clientSocket = null;
    private static File directory = null;
    private static final long id = System.currentTimeMillis();
    private static boolean connectSuccess = false;


    public static void main(String[] args) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = null;
        boolean stopCommunication = false;
        final String BIG_DIV = "\n======================================================\n";
        final String SMALL_DIV = "\n---------------------\n";
        Scanner scanner = new Scanner(System.in);

        setDirectory();
        System.out.println(BIG_DIV);

        // get server's IP address
        System.out.print("Enter server's IP address: ");
        String serverIPAddress = scanner.nextLine();
        System.out.println("Setting up the connection...");

        try {
            // authentication
//            authenticate(serverIPAddress);

            // authentication success
            while (!stopCommunication) {
                // create a socket for client with the local host and port 1111
                clientSocket = new Socket(serverIPAddress, 1111);
                clientSocket.setSoTimeout(30000);

                if(!connectSuccess) {
                    date = new Date();
                    System.out.println("Connection established at " +
                            dateFormat.format(date));
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
            System.out.println("\nError: server is busy.");
        }
        catch (FileNotFoundException e) {
            System.out.println("\nError: cannot create or find file.");
        }
        catch(IOException e) {
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
     * check to see if the command is valid
     * valid set of commands:
     *      quit
     *      list
     *      list-me
     *      help
     *      download | [fileName]
     *      upload | [filePath]
     *
     * NOTE: "stay" is also a valid command,
     * but is reserved for system call and is inaccessible to user
     *
     * @param clientCommand: command from client
     * @param delimiter: regex to split
     * @return true if valid command, false otherwise
     */
    private static boolean isValidCommand(String clientCommand, String delimiter) {
        String[] commandTokens = clientCommand.split(delimiter);
        boolean isQuit = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("quit");
        boolean isList = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("list");
        boolean isListMe = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("list-me");
        boolean isHelp = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("help");
        boolean isDownload = commandTokens.length == 2 &&
                commandTokens[0].equalsIgnoreCase("download");
        boolean isUpload = commandTokens.length == 2 &&
                commandTokens[0].equalsIgnoreCase("upload");

        return isQuit || isList || isListMe || isHelp || isDownload || isUpload;
    }


    /***
     * method: communicate
     *
     * communication session between server and client
     *
     * @return true if stop communication, false if not
     * @throws IOException
     */
    private static boolean communicate() throws IOException {
        final String DELIMITER = "\\s+\\|\\s+";
        String command = getCommand(DELIMITER);
        String[] commandComponents = command.split(DELIMITER);
        final boolean STOP_CONNECTION_AFTER_THIS = true;

        if(commandComponents[0].equalsIgnoreCase("quit")) {
            quit(commandComponents[0]);
            return STOP_CONNECTION_AFTER_THIS;
        }
        else if(commandComponents[0].equalsIgnoreCase("list")) {
            list(commandComponents[0]);
        }
        else if(commandComponents[0].equalsIgnoreCase("list-me")) {
            listMe();
        }
        else if(commandComponents[0].equalsIgnoreCase("help")) {
            help();
        }
        else if(commandComponents[0].equalsIgnoreCase("download")) {
            download(command, commandComponents);
        }
        else {
            upload(command, commandComponents);
        }

        return !STOP_CONNECTION_AFTER_THIS;
    }


    /***
     * method: getCommand
     *
     * get the valid command from the user
     *
     * @param delimiter: regex to split
     * @return a valid command
     */
    private static String getCommand(String delimiter) {
        Scanner input = new Scanner(System.in);
        String command = "";
        boolean isValid = false;

        do {
            System.out.print("[You]:    ");
            command = input.nextLine();

            if(!isValidCommand(command, delimiter)) {
                System.out.println(">> Invalid command. " +
                        "Please enter a correct command or " +
                        "enter \"help\" for help.\n");
            }
            else {
                isValid = true;
            }

        } while(!isValid);

        return command;
    }


    /***
     * method: quit
     *
     * end the session
     *
     * @param command: client's command
     * @throws IOException
     */
    private static void quit(String command) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        printWriter.println(command);
        printWriter.flush();
        printWriter.close();
    }


    /***
     * method: list
     *
     * list command from user
     * send to server to get back a list of files the server contains
     *
     * @param command: client's command
     * @throws IOException
     */
    private static void list(String command) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        Scanner serverInput = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
        String messageReceived = "";

        printWriter.println(command);
        printWriter.flush();

        if (serverInput.hasNextLine()) {
            messageReceived = serverInput.nextLine();
            System.out.println("\n[Server]: " + messageReceived + "\n");
        }

        printWriter.close();
    }


    /***
     * method: listMe
     *
     * list-me command
     * show a list of files the client contains
     */
    private static void listMe() throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        File[] files = directory.listFiles();
        StringBuilder listOfFiles = new StringBuilder();

        System.out.print(">> ");

        if(files == null) {
            System.out.println("Error: cannot find files directory.");
        }
        else if(files.length == 0) {
            System.out.println("Empty files directory");
        }
        else {
            for (int i = 0; i < files.length; i++) {
                listOfFiles.append(files[i].getName());

                if (i != files.length - 1) {
                    listOfFiles.append(", ");
                }
            }

            System.out.println(listOfFiles.toString());
        }
        System.out.println();

        // keep connection alive
        printWriter.println("stay");
        printWriter.flush();
        printWriter.close();
    }


    /***
     * method: download
     *
     * download [file] command
     * download an existing file from the server and save it
     *
     * @param command: client's command
     * @param commandComponents: parts of command
     * @throws IOException
     */
    private static void download(String command, String[] commandComponents) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner serverInput = new Scanner(new InputStreamReader(inputStream));
        String messageReceived = "";
        byte[] byteBlock = new byte[1];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        String fileToDownloadName = commandComponents[1];

        printWriter.println(command);
        printWriter.flush();

        // confirmation message
        messageReceived = serverInput.nextLine();

        // error
        if(!messageReceived.contains(fileToDownloadName)) {
            System.out.println("\n[Server]: " + messageReceived);
        }
        // valid file to download
        else {
            fileToDownloadName = directory.getAbsolutePath() + "/" + fileToDownloadName;
            File downloadedFile = new File(fileToDownloadName);
            fileOutputStream = new FileOutputStream(downloadedFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            int byteRead = inputStream.read(byteBlock, 0, byteBlock.length);

            System.out.println(">> Downloading...");

            while (byteRead >= 0) {
                byteArrayOutputStream.write(byteBlock);
                byteRead = inputStream.read(byteBlock);
            }

            bufferedOutputStream.write(byteArrayOutputStream.toByteArray());
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            printWriter.close();

            System.out.printf(">> New file saved at \"%s\"\n", downloadedFile.getAbsolutePath());
        }
        System.out.println();
    }


    /***
     * method: upload
     *
     * upload [filePath] command
     *
     * let the client upload a file using absolute path
     *
     * @param command: client's command
     * @param commandComponents: parts of command
     * @throws IOException
     */
    private static void upload(String command, String[] commandComponents) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        String fileName = commandComponents[1];
        String fileNameFormattedPath = fileName.replace("\\", "\\\\");
        File uploadedFile = new File(fileNameFormattedPath);
        byte[] byteArray = new byte[(int) uploadedFile.length()];
        FileInputStream fileInputStream = null;
        PrintWriter printWriter = new PrintWriter(outputStream, true);

        try {
            fileInputStream = new FileInputStream(uploadedFile);

            // send command to the server
            printWriter.println(command);
            printWriter.flush();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            System.out.printf(">> Uploading \"%s\" ...\n", fileName);

            bufferedInputStream.read(byteArray, 0, byteArray.length);
            bufferedOutputStream.write(byteArray, 0, byteArray.length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

            System.out.printf(">> Complete uploading \"%s\"\n", uploadedFile.getAbsolutePath());
            System.out.println();

        }
        catch (FileNotFoundException e) {
            System.out.println(">> Invalid file path.");
            System.out.println();

            // keep connection alive
            printWriter.println("stay");
            printWriter.flush();
            printWriter.close();
        }

    }


    /***
     * method: help
     *
     * display all possible commands to the user
     *
     * @throws IOException
     */
    private static void help() throws IOException {
        System.out.println(">> All commands: ");
        System.out.println("       quit:            end session");
        System.out.println("       list:            list all files of the server");
        System.out.println("       list-me:         list all files of the client");
        System.out.println("       help:            list all possible commands accessible by client");
        System.out.println("       download | file: download the existing \"file\" from the server");
        System.out.println("       upload | file:   upload the \"file\" (absolute path) to the server");
        System.out.println();

        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        printWriter.println("stay");
        printWriter.flush();
        printWriter.close();
    }


    /***
     * method: setDirectory
     *
     * set the Files Directory to store all files
     * remove the "\src" in the path when run from the command line environment
     */
    private static void setDirectory() {
        directory = new File("Client/FilesDirectory");
        String absolutePath = directory.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
        absolutePath = absolutePath.replace("/src", "");
        absolutePath = absolutePath.replace("/Client/Client", "/Client");
        directory = new File(absolutePath);
    }


    /***
     * method: authenticate
     *
     * client authenticates server with keys
     * if server is busy -> return false
     * otherwise -> key exchange
     *
     * @param serverIP: IP address of server
     * @return true if success, false if not
     * @throws IOException if socket error
     */
    private static boolean authenticate(String serverIP) throws IOException {
        boolean authenticateSuccess = false;
        clientSocket = new Socket(serverIP, 1111);

        return authenticateSuccess;
    }

}
