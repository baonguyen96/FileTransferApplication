import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Server {
    private Socket clientSocket = null;
    private File filesDirectory = null;
    private File src = null;
    private final String PRIVATE_KEY = getKey("PrivateKey.txt");
    private final String PUBLIC_KEY = getKey("PublicKey.txt");
    private boolean isBusy = false;
    private boolean hasSentCertificate = false;
    private boolean hasReceivedKeys = false;
    private long masterKey = 0;
    private int sequenceNumber = 0;
    private int totalInvalidMessagesReceived = 0;
    private static final int MAX_INVALID_MESSAGES_ALLOWED = 5;
    private String clientIpAddress = null;
    private String clientId = null;
    private final String CERTIFICATION = "CA-certificate.crt";
    private final String DELIMITER = "\\s+\\|\\s+";


    private Server() {
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.exec();
    }


    /***
     * method: exec
     *
     * execute the Server and control the flow of the program
     */
    private void exec() {
        // error with keys then stop
        if(PRIVATE_KEY == null || PUBLIC_KEY == null) {
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = null;
        boolean stopCommunication = false;
        final String BIG_DIV = "\n======================================================\n";
        final String SMALL_DIV = "\n---------------------\n";

        setDirectories();

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

                // end if detect intruder
                if(isIntruderDetected()) {
                    System.out.println("\nWarning: intruder detected. Abort connection.");
                    clientSocket.close();
                    serverSocket.close();
                    break;
                }

                clientSocket = serverSocket.accept();

                // make sure to talk to the same client over several sessions
                if(!authenticate()) {
                    clientSocket.close();
                    continue;
                }
                else if(hasSentCertificate && !hasReceivedKeys) {
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
     * method: isIntruderDetected
     *
     * intruder is detected if the total invalid messages received
     * is more than the allowed threshold
     *
     * @return true if detect intruder, false otherwise
     */
    private boolean isIntruderDetected() {
        return totalInvalidMessagesReceived > MAX_INVALID_MESSAGES_ALLOWED;
    }


    /***
     * method: isValidCommand
     *
     * validate the command
     *
     * @param clientCommand: client's command
     *
     * @return true if valid command, false if not
     */
    private boolean isValidCommand(String clientCommand) {
        String[] commandTokens = clientCommand.split(DELIMITER);
        boolean isQuit = commandTokens.length == 2 &&
                commandTokens[1].equalsIgnoreCase("quit");
        boolean isList = commandTokens.length == 1 &&
                commandTokens[0].equalsIgnoreCase("list");
        boolean isStay = commandTokens.length == 2 &&
                commandTokens[1].equalsIgnoreCase("stay");
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
    private boolean communicate() throws IOException {
        String receivedCommand = "";
        String[] commandTokens = null;
        final boolean STOP_CONNECTION_AFTER_THIS = true;
        final boolean CONTINUE_CONNECTION_AFTER_THIS = false;
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));

        // offline
        if(!receivedInput.hasNextLine()) {
            System.out.println(">> Client is offline.");
            return STOP_CONNECTION_AFTER_THIS;
        }

        receivedCommand = receivedInput.nextLine();

        if(!Message.validateMessageSequenceNumber(++sequenceNumber, receivedCommand)) {
            handleInvalidMessages();
            return CONTINUE_CONNECTION_AFTER_THIS;
        }
        else if(!isValidCommand(receivedCommand)) {
            return CONTINUE_CONNECTION_AFTER_THIS;
        }

        // valid command
        commandTokens = receivedCommand.split(DELIMITER);
        displayClientCommand(commandTokens);

        // switch
        if(commandTokens[1].equalsIgnoreCase("quit")) {
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
        else if(commandTokens[1].equalsIgnoreCase("stay")){
            // stay -> don't do anything
        }

        return CONTINUE_CONNECTION_AFTER_THIS;
    }


    /***
     * method: displayClientCommand
     *
     * print the client's command to the screen
     *
     * @param commandTokens: components of the client's command
     */
    private void displayClientCommand(String[] commandTokens) {
        System.out.print("[Client]: ");
        for(int i = 1; i < commandTokens.length; i++) {
            System.out.print(commandTokens[i]);

            if(i != commandTokens.length - 1) {
                System.out.print(DELIMITER);
            }
        }
        System.out.println();
    }


    /***
     * method: list
     *
     * list command
     * send to client a list of all files the server contains
     *
     * @throws IOException
     */
    private void list() throws IOException {
        File[] files = filesDirectory.listFiles();
        StringBuilder messageToSend = new StringBuilder();
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);

        if(files == null) {
            messageToSend.append("Error: cannot find files filesDirectory.");

            System.out.println(">> " + messageToSend.toString());
            printWriter.println(messageToSend.toString());
            printWriter.flush();
            printWriter.close();
        }
        else if (files.length == 0) {
            messageToSend.append("Empty files filesDirectory.");

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
    private void clientDownload(String[] commandTokens) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        String fileToSendName = commandTokens[1];

        try {
            File fileToSend = new File(filesDirectory.getAbsolutePath()
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
    private void clientUpload(String[] commandTokens) throws IOException {
        String filePath = commandTokens[1].replace("\\", "/");
        String[] uploadedFilePathComponents = filePath.split("/");
        String uploadedFileName = uploadedFilePathComponents[uploadedFilePathComponents.length - 1];
        File receivingFile = new File(filesDirectory.getAbsolutePath() +
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
     * method: sendCertificate
     *
     * send the CA certificate to client
     * @throws IOException
     */
    private void sendCertificate() throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;

        try {
            String certificatePath = src.getAbsolutePath() + "/" + CERTIFICATION;
            File fileToSend = new File(certificatePath);
            byte[] byteArray = new byte[(int) fileToSend.length()];
            fileInputStream = new FileInputStream(fileToSend);
            bufferedInputStream = new BufferedInputStream(fileInputStream);

            // confirm
//            printWriter.println("sending certificate");
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, "sending certificate"));
            printWriter.flush();

            // file transfer (does not have sequence number yet)
            bufferedInputStream.read(byteArray, 0, byteArray.length);
            bufferedOutputStream.write(byteArray, 0, byteArray.length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

        }
        catch (FileNotFoundException e) {
//            printWriter.println("error");
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, "error"));
            printWriter.flush();
            printWriter.close();
        }

    }


    /***
     * method: authenticate
     *
     * client authenticates server with keys
     * prevent multiple clients to connect simultaneously
     *
     * @throws IOException
     */
    private boolean authenticate() throws IOException {

        final boolean AUTHENTICATE_SUCCESS = true;
        final boolean AUTHENTICATE_FAILURE = false;
        boolean authenticateSuccess = false;
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));
        String clientMessage = receivedInput.nextLine();

        // first time connect (certificate)
        if(!hasSentCertificate) {
            if(!Message.validateMessageSequenceNumber(sequenceNumber, clientMessage)) {
                return AUTHENTICATE_FAILURE;
            }
            else if(clientMessage.equals("0 | requestCertificate")) {
                sendCertificate();
                hasSentCertificate = true;
                return AUTHENTICATE_SUCCESS;
            }
            else {
                return AUTHENTICATE_FAILURE;
            }
        }
        // get keys
        else if(!hasReceivedKeys) {
            /*
             * note: encrypted client message
             * -> have to decrypt and verify before save
             */
            // ip
            clientIpAddress = clientSocket.getInetAddress().getHostAddress();
//            clientId = clientMessage;
//            masterKey = Long.parseLong(receivedInput.nextLine());

            // id
            if(!Message.validateMessageSequenceNumber(++sequenceNumber, clientMessage)) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }
            else {
                clientId = clientMessage.split(DELIMITER)[1];
            }

            // master key
            clientMessage = receivedInput.nextLine();

            if(!Message.validateMessageSequenceNumber(++sequenceNumber, clientMessage)) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }
            else {
                masterKey = Long.parseLong(clientMessage.split(DELIMITER)[1]);
            }

            authenticateSuccess = AUTHENTICATE_SUCCESS;
            hasReceivedKeys = true;
        }
        // already send certificate and receive keys
        else {
            /*
             * encrypted messages
             */
            if(!Message.validateMessageSequenceNumber(++sequenceNumber, clientMessage)) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }

            String id = clientMessage.split(DELIMITER)[1];
            authenticateSuccess = id.equals(clientId) &&
                    clientSocket.getInetAddress().getHostAddress().equals(clientIpAddress);
            String confirmMessage = authenticateSuccess ? "ok" : "busy";
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, confirmMessage));
            printWriter.flush();
        }

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
    private String getKey(String keyFileName) {
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


    /***
     * method: setDirectories
     *
     * set the Files Directory to store all files
     * remove the "\src" in the path when run from the command line environment
     */
    private void setDirectories() {
        filesDirectory = new File("Server/FilesDirectory");
        String absolutePath = filesDirectory.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
        absolutePath = absolutePath.replace("/src", "");
        absolutePath = absolutePath.replace("/Server/Server", "/Server");
        filesDirectory = new File(absolutePath);


        src = new File("Server/src");
        absolutePath = src.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
        absolutePath = absolutePath.replace("Server/src/Server/src", "Server/src");
        src = new File(absolutePath);
    }


    /***
     * method: handleInvalidMessages
     *
     * increase the total invalid messages received count
     * decrease the sequence number to rollback
     */
    private void handleInvalidMessages() {
        totalInvalidMessagesReceived++;
        sequenceNumber--;
    }
}

