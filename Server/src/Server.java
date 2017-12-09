import javax.crypto.Cipher;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Scanner;


public class Server extends Peer {
    private final String PRIVATE_KEY = getKey("PrivateKey.txt");
    private final String PUBLIC_KEY = getKey("PublicKey.txt");
    private boolean isBusy = false;
    private boolean hasSentCertificate = false;
    private boolean hasReceivedKeys = false;
    private String clientId = null;
    private boolean detectsAttackOnAuthentication = false;


    protected Server() {
        super(SERVER);
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
    protected void exec() {
        // error with keys then stop
        if (PRIVATE_KEY == null || PUBLIC_KEY == null) {
            return;
        }

        boolean stopCommunication = false;
        setDirectories();

        try {
            System.out.println(BIG_DIV);
            System.out.println("IP address: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Waiting for connection...");

            ServerSocket serverSocket = new ServerSocket(1111);
            serverSocket.setSoTimeout(60000);

            while (!stopCommunication) {

                printKeys();

                // end if detect intruder
                if (isIntruderDetected()) {
                    System.out.println(SMALL_DIV);
                    System.out.println("Warning: Intruder detected. Abort connection.");
                    serverSocket.close();
                    break;
                }

                clientSocket = serverSocket.accept();

                // make sure to talk to the same client over several sessions
                if (!authenticate()) {
                    clientSocket.close();
                    continue;
                }
                else if (hasSentCertificate && !hasReceivedKeys) {
                    continue;
                }

                isBusy = notifyConnectionSuccess(isBusy);
                stopCommunication = communicate();
                clientSocket.close();

            }
            serverSocket.close();

        }
        catch (UnknownHostException e) {
            System.out.println(SMALL_DIV);
            System.out.println("Error: Unknown IP address.");
        }
        catch (SocketTimeoutException e) {
            System.out.println(SMALL_DIV);
            System.out.println("Error: Time out.");
        }
        catch (FileNotFoundException e) {
            System.out.println(SMALL_DIV);
            System.out.println("Error: File not found.");
        }
        catch (IOException e) {
            System.out.println(SMALL_DIV);
            System.out.println("Error: Sockets corrupted.");
        }
        catch (Exception e) {
            System.out.println(SMALL_DIV);
            System.out.println("Error: Something went wrong.");
            e.printStackTrace();
        }
        finally {
            notifyConnectionEnd(isBusy);
        }
    }


    /***
     * method: isValidCommand
     *
     * validate the command (in term of client's terminology)
     * commands are appended with the sequence number in the front
     *
     * @param clientCommand: client's command
     * @return true if valid command, false if not
     */
    protected boolean isValidCommand(String clientCommand) {
        String[] commandTokens = clientCommand.split(DELIMITER);
        boolean isQuit = commandTokens.length == 2 &&
                commandTokens[1].equalsIgnoreCase("quit");
        boolean isList = commandTokens.length == 2 &&
                commandTokens[1].equalsIgnoreCase("list");
        boolean isStay = commandTokens.length == 2 &&
                commandTokens[1].equalsIgnoreCase("stay");
        boolean isDownload = commandTokens.length == 3 &&
                commandTokens[1].equalsIgnoreCase("download");
        boolean isUpload = commandTokens.length == 3 &&
                commandTokens[1].equalsIgnoreCase("upload");

        return isQuit || isList || isStay || isDownload || isUpload;
    }


    /***
     * method: communicate
     *
     * communication session between server and client
     * run commands according to client's input
     *
     * @return true if stop communication, false if not
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected boolean communicate() throws IOException {
        String receivedCommand = "";
        String[] commandTokens = null;
        final boolean STOP_CONNECTION_AFTER_THIS = true;
        final boolean CONTINUE_CONNECTION_AFTER_THIS = false;
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));

        // client is offline
        if (!receivedInput.hasNextLine()) {
            System.out.println(">> Client is offline.");
            return STOP_CONNECTION_AFTER_THIS;
        }

        receivedCommand = receivedInput.nextLine();

        // errors
        if (!Message.validateMessageSequenceNumber(++sequenceNumber, receivedCommand)) {
            handleInvalidMessages();
            return CONTINUE_CONNECTION_AFTER_THIS;
        }
        else if (!isValidCommand(receivedCommand)) {
            return CONTINUE_CONNECTION_AFTER_THIS;
        }
        else if (receivedCommand.contains("stay")) {
            return CONTINUE_CONNECTION_AFTER_THIS;
        }

        // valid command
        commandTokens = receivedCommand.split(DELIMITER);
        displayPeerMessage(receivedCommand);

        // switch
        if (commandTokens[1].equalsIgnoreCase("quit")) {
            return STOP_CONNECTION_AFTER_THIS;
        }
        else if (commandTokens[1].equalsIgnoreCase("list")) {
            list();
        }
        else if (commandTokens[1].equalsIgnoreCase("download")) {
            clientDownload(commandTokens);
        }
        else if (commandTokens[1].equalsIgnoreCase("upload")) {
            clientUpload(commandTokens);
        }
        else if (commandTokens[1].equalsIgnoreCase("stay")) {
            // stay -> don't do anything
        }

        return CONTINUE_CONNECTION_AFTER_THIS;
    }


    /***
     * method: list
     *
     * list command
     * send to client a list of all files the server contains
     *
     * @throws IOException
     */
    protected void list() throws IOException {
        File[] files = filesDirectory.listFiles();
        StringBuilder messageToSend = new StringBuilder();
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);

        if (files == null) {
            messageToSend.append("Error: Cannot find files filesDirectory.");
            System.out.println(">> " + messageToSend.toString());
        }
        else if (files.length == 0) {
            messageToSend.append("Empty files filesDirectory.");
            System.out.println(">> " + messageToSend.toString());
        }
        else {
            System.out.println(">> Sending list of files...");

            for (int i = 0; i < files.length; i++) {
                messageToSend.append(files[i].getName());

                if (i != files.length - 1) {
                    messageToSend.append(", ");
                }
            }

            System.out.println(">> Complete sending list of files");
        }

        // send to client
        System.out.println();
        printWriter.println(Message.appendMessageSequence(++sequenceNumber, messageToSend.toString()));
        printWriter.flush();
        printWriter.close();
    }


    /***
     * method: clientDownload
     *
     * download | file
     * let client download files from the library
     *
     * @param commandTokens: parts of client's command
     * @throws IOException
     */
    protected void clientDownload(String[] commandTokens) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        String fileToSendName = commandTokens[2];

        try {
            File fileToSend = new File(filesDirectory.getAbsolutePath()
                    + "/" + fileToSendName);
            byte[] byteArray = new byte[(int) fileToSend.length()];
            fileInputStream = new FileInputStream(fileToSend);
            bufferedInputStream = new BufferedInputStream(fileInputStream);

            // confirmation message
            String confirmationMessage = String.format("Sending \"%s\" ...", fileToSendName);
            System.out.println(">> " + confirmationMessage);
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, confirmationMessage));
            printWriter.flush();

            // file transfer
            bufferedInputStream.read(byteArray, 0, byteArray.length);
            byteArray = Message.appendMessageSequence(++sequenceNumber, byteArray);
            byteArray = AES.encrypt(byteArray, encryptionKey);
            bufferedOutputStream.write(byteArray, 0, byteArray.length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

            System.out.printf(">> Complete sending \"%s\"\n\n", fileToSendName);
        }
        catch (FileNotFoundException e) {
            String error = "Error: requested file does not exist.";
            System.out.printf("[You]:    %s\n\n", error);
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, error));
            printWriter.flush();
            printWriter.close();
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
    protected void clientUpload(String[] commandTokens) throws IOException {
        String filePath = commandTokens[2].replace("\\", "/");
        String[] uploadedFilePathComponents = filePath.split("/");
        String uploadedFileName = uploadedFilePathComponents[uploadedFilePathComponents.length - 1];
        File receivingFile = new File(filesDirectory.getAbsolutePath() +
                "/" + uploadedFileName);
        InputStream inputStream = clientSocket.getInputStream();
        byte[] byteBlock = new byte[1];
        FileOutputStream fileOutputStream = new FileOutputStream(receivingFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        System.out.printf(">> Receiving \"%s\" ...\n", uploadedFileName);

        int byteRead = inputStream.read(byteBlock, 0, byteBlock.length);
        while (byteRead >= 0) {
            byteArrayOutputStream.write(byteBlock);
            byteRead = inputStream.read(byteBlock);
        }

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        try {
            byteArray = AES.decrypt(byteArray, encryptionKey);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create Pi Face Device", e);
        }

        if (!Message.validateMessageSequenceNumber(++sequenceNumber, byteArray)) {
            handleInvalidMessages();
            bufferedOutputStream.close();
            inputStream.close();
            System.out.printf(">> Oops! Something went wrong. Cannot save \"%s\"\n", uploadedFileName);
        }
        else {
            byteArray = Message.extractMessage(byteArray);
            bufferedOutputStream.write(byteArray);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            inputStream.close();
            System.out.printf(">> Complete saving \"%s\"\n\n", uploadedFileName);
        }
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
            String certificatePath = src.getAbsolutePath() + "/CA-certificate.crt";
            File fileToSend = new File(certificatePath);
            byte[] byteArray = new byte[(int) fileToSend.length()];
            fileInputStream = new FileInputStream(fileToSend);
            bufferedInputStream = new BufferedInputStream(fileInputStream);

            // confirm
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, "Sending certificate"));
            printWriter.flush();

            // file transfer
            bufferedInputStream.read(byteArray, 0, byteArray.length);
            byteArray = Message.appendMessageSequence(++sequenceNumber, byteArray);

            /*
             * Has to do a println here to flush the buffer if something is still left
             * from the SYN flood attack. It is not perfect to track the program
             * when displaying this message here, but for limited time that we have, this works.
             * We will try to clean up this later.
             */
            if(detectsAttackOnAuthentication) {
                System.out.println(SMALL_DIV);
                System.out.println("Warning: authentication problem. Reset connection.");
                System.out.println(SMALL_DIV);
                detectsAttackOnAuthentication = false;
            }

            bufferedOutputStream.write(byteArray, 0, byteArray.length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }
        catch (FileNotFoundException e) {
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
     * @return true if authenticate success, false if not
     * @throws IOException
     */
    private boolean authenticate() throws IOException {
        final boolean AUTHENTICATE_SUCCESS = true;
        final boolean AUTHENTICATE_FAILURE = false;
        boolean authenticateSuccess = false;
        OutputStream outputStream = clientSocket.getOutputStream();;
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));
        String clientMessage = null;

        // DDoS -> reset everything + set flag to detect attack
        if(!receivedInput.hasNextLine()) {
            if(hasSentCertificate && !hasReceivedKeys) {
                detectsAttackOnAuthentication = true;
                hasSentCertificate = false;
                masterKey = encryptionKey = signatureKey = null;
//                sequenceNumber = 0;
            }
            return AUTHENTICATE_FAILURE;
        }
        else {
            clientMessage = receivedInput.nextLine();
        }

        // first time connect -> certificate request + initial sequence number
        if (!hasSentCertificate) {
            if (clientMessage.matches("\\d+ \\| Request certificate")) {
                sequenceNumber = Integer.parseInt(clientMessage.split(DELIMITER)[0]);
                sendCertificate();
                hasSentCertificate = true;
                return AUTHENTICATE_SUCCESS;
            }
            else {
                sequenceNumber = 0;
                return AUTHENTICATE_FAILURE;
            }
        }
        // get keys
        else if (!hasReceivedKeys) {
            // id
            if (!Message.validateMessageSequenceNumber(++sequenceNumber, clientMessage)) {
                handleInvalidMessages();
                sequenceNumber = 0;
                return AUTHENTICATE_FAILURE;
            }
            clientId = clientMessage.split(DELIMITER)[1];

            // master key
            clientMessage = receivedInput.nextLine();

            if (!Message.validateMessageSequenceNumber(++sequenceNumber, clientMessage)) {
                handleInvalidMessages();
                sequenceNumber = 0;
                return AUTHENTICATE_FAILURE;
            }

            try {
                PrivateKey serverPrivateKey = stringToPrivateKey(PRIVATE_KEY);
                masterKey = privateDecrypt(clientMessage.split(DELIMITER)[1], serverPrivateKey);
                encryptionKey = AES.modifyKey(masterKey, 1);
                signatureKey = AES.modifyKey(masterKey, 2);
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to decrypt the key", e);
            }

            // send confirmation message
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, "ok"));
            authenticateSuccess = AUTHENTICATE_SUCCESS;
            hasReceivedKeys = true;
        }
        // already send certificate and receive keys
        else {
            // update keys
            encryptionKey = AES.modifyKey(encryptionKey, 2);
            signatureKey = AES.modifyKey(signatureKey, 2);

            if (!Message.validateMessageSequenceNumber(++sequenceNumber, clientMessage)) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }

            String id = clientMessage.split(DELIMITER)[1];
            authenticateSuccess = id.equals(clientId);
            String confirmMessage = authenticateSuccess ? "ok" : "busy";
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, confirmMessage));
            printWriter.flush();
        }

        return authenticateSuccess;
    }


    /***
     * method: stringToPrivateKey
     *
     * Get Server's private key
     *
     * @param key: private key as String
     * @return private key object
     */
    private static PrivateKey stringToPrivateKey(String key) throws Exception {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }


    /***
     * method: privateDecrypt
     *
     * Use Server's private key to decrypt the master key.
     *
     * @param cipherText: encrypted message
     * @param privateKey: server's private key
     * @return the original text
     */
    private static String privateDecrypt(String cipherText, PrivateKey privateKey) throws Exception {
        String[] strArr = cipherText.split(" ");
        int len = strArr.length;
        byte[] clone = new byte[len];
        for (int i = 0; i < len; i++) {
            clone[i] = Byte.parseByte(strArr[i]);
        }
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] bt_original = cipher.doFinal(clone);
        return new String(bt_original);
    }

}

