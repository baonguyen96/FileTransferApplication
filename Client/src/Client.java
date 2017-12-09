import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Scanner;


public class Client extends Peer {
    private final long id = System.currentTimeMillis();
    private boolean connectSuccess = false;
    protected boolean hasReceivedCertificate = false;
    private boolean hasSentKey = false;
    private final String CERTIFICATION = "CA-certificate.crt";
    private PublicKey serverPublicKey = null;


    protected Client() {
        super(CLIENT);
        masterKey = AES.getRandomString(16);
        sequenceNumber = (int)(Math.random() * 1000);
    }


    public static void main(String[] args) {
        Client client = new Client();
        client.exec();
    }


    /***
     * method: exec
     *
     * execute the Client and control the flow of the program
     */
    protected void exec() {
        boolean stopCommunication = false;
        Scanner scanner = new Scanner(System.in);

        setDirectories();
        System.out.println(BIG_DIV);

        try {
            // get server's IP address
            System.out.print("Enter server's IP address: ");
            String serverIPAddress = scanner.nextLine();
            System.out.println("Setting up the connection...");

            while (!stopCommunication) {

                printKeys();

                // end if detect intruder
                if (isIntruderDetected()) {
                    System.out.println(SMALL_DIV);
                    System.out.println("Warning: Intruder detected. Abort connection.");
                    break;
                }

                clientSocket = new Socket(serverIPAddress, 1111);
                clientSocket.setSoTimeout(30000);

                // authentication
                if (!authenticate()) {
                    if (!connectSuccess) {
                        System.out.println(SMALL_DIV);
                        System.out.println("Error: Access denied.");
                        clientSocket.close();
                        break;
                    }

                }
                else if (hasReceivedCertificate && !hasSentKey) {
                    continue;
                }

                connectSuccess = notifyConnectionSuccess(connectSuccess);
                stopCommunication = communicate();
                clientSocket.close();

            }

        }
        catch (UnknownHostException e) {
            System.out.println(SMALL_DIV);
            System.out.println("Error: Unknown IP address.");
        }
        catch (SocketTimeoutException e) {
            System.out.println(SMALL_DIV);
            System.out.println("Error: Server is busy.");
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
           notifyConnectionEnd(connectSuccess);
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
     * NOTE: this only validate the raw input from the user
     * (before the command is appended with the sequence number)
     *
     * @param clientCommand: command from client
     * @return true if valid command, false otherwise
     */
    private boolean isValidCommand(String clientCommand) {
        String[] commandTokens = clientCommand.split(DELIMITER);
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
    protected boolean communicate() throws IOException {
        String command = getCommand();
        String[] commandComponents = command.split(DELIMITER);
        final boolean STOP_CONNECTION_AFTER_THIS = true;
        final boolean CONTINUE_CONNECTION_AFTER_THIS = false;

        if (commandComponents[0].equalsIgnoreCase("quit")) {
            quit(commandComponents[0]);
            return STOP_CONNECTION_AFTER_THIS;
        }
        else if (commandComponents[0].equalsIgnoreCase("list")) {
            list(commandComponents[0]);
        }
        else if (commandComponents[0].equalsIgnoreCase("list-me")) {
            listMe();
        }
        else if (commandComponents[0].equalsIgnoreCase("help")) {
            help();
        }
        else if (commandComponents[0].equalsIgnoreCase("download")) {
            download(command, commandComponents);
        }
        else {
            upload(command, commandComponents);
        }

        return CONTINUE_CONNECTION_AFTER_THIS;
    }


    /***
     * method: getCommand
     *
     * get the valid command from the user
     *
     * @return a valid command
     */
    protected String getCommand() {
        Scanner input = new Scanner(System.in);
        String command = "";
        boolean isValid = false;

        do {
            System.out.print("[You]:    ");
            command = input.nextLine();

            if (!isValidCommand(command)) {
                System.out.println(">> Invalid command. " +
                        "Please enter a correct command or " +
                        "enter \"help\" for help.\n");
            }
            else {
                isValid = true;
            }

        } while (!isValid);

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
    protected void quit(String command) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        printWriter.println(Message.appendMessageSequence(++sequenceNumber, command));
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
    protected void list(String command) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        Scanner serverInput = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
        String messageReceived = "";

        printWriter.println(Message.appendMessageSequence(++sequenceNumber, command));
        printWriter.flush();

        // lost message
        if (!serverInput.hasNextLine()) {
            printWriter.close();
            return;
        }

        messageReceived = serverInput.nextLine();

        if (!Message.validateMessageSequenceNumber(++sequenceNumber, messageReceived)) {
            handleInvalidMessages();
        }
        else {
            displayPeerMessage(messageReceived);
        }

        System.out.println();
        printWriter.close();
    }


    /***
     * method: listMe
     *
     * list-me command
     * show a list of files the client contains
     */
    protected void listMe() throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        File[] files = filesDirectory.listFiles();
        StringBuilder listOfFiles = new StringBuilder();

        System.out.print(">> ");

        if (files == null) {
            System.out.println("Error: Cannot find files fileDirectory.");
        }
        else if (files.length == 0) {
            System.out.println("Empty files fileDirectory");
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
        printWriter.println(Message.appendMessageSequence(++sequenceNumber, "stay"));
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
    protected void download(String command, String[] commandComponents) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner serverInput = new Scanner(new InputStreamReader(inputStream));
        String messageReceived = "";
        byte[] byteBlock = new byte[1];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        String fileToDownloadName = commandComponents[1];

        printWriter.println(Message.appendMessageSequence(++sequenceNumber, command));
        printWriter.flush();

        // lost message
        if (!serverInput.hasNextLine()) {
            return;
        }

        // confirmation message
        messageReceived = serverInput.nextLine();

        // errors on confirmation
        if (!Message.validateMessageSequenceNumber(++sequenceNumber, messageReceived)) {
            handleInvalidMessages();
        }
        else if (!messageReceived.contains(fileToDownloadName)) {
            displayPeerMessage(messageReceived);
        }
        // valid confirmation -> able to download
        else {
            fileToDownloadName = filesDirectory.getAbsolutePath() + "/" + fileToDownloadName;
            File downloadedFile = new File(fileToDownloadName);
            fileOutputStream = new FileOutputStream(downloadedFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            // read the byte stream of file, appending with the sequence number
            int byteRead = inputStream.read(byteBlock, 0, byteBlock.length);
            while (byteRead >= 0) {
                byteArrayOutputStream.write(byteBlock);
                byteRead = inputStream.read(byteBlock);
            }
            byte[] byteStream = byteArrayOutputStream.toByteArray();

            // decrypt
            byteStream = AES.decrypt(byteStream, encryptionKey);

            // validate sequence number
            if (!Message.validateMessageSequenceNumber(++sequenceNumber, byteStream)) {
                handleInvalidMessages();
            }
            else {
                System.out.println(">> Downloading...");
                byteStream = Message.extractMessage(byteStream);
                bufferedOutputStream.write(byteStream);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                printWriter.close();

                System.out.printf(">> New file saved at \"%s\"\n", downloadedFile.getAbsolutePath());
            }
        }
        System.out.println();
    }


    /***
     * method: upload
     *
     * upload [filePath] command
     * let the client upload a file using absolute path
     *
     * @param command: client's command
     * @param commandComponents: parts of command
     * @throws IOException
     */
    protected void upload(String command, String[] commandComponents) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        String fileName = commandComponents[1];
        String fileNameFormattedPath = fileName.replace("\\", "\\\\");
        File uploadedFile = new File(fileNameFormattedPath);
        byte[] byteArray = new byte[(int) uploadedFile.length()];
        FileInputStream fileInputStream = null;
        PrintWriter printWriter = new PrintWriter(outputStream, true);

        try {
            fileInputStream = new FileInputStream(uploadedFile);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            printWriter.println(Message.appendMessageSequence(++sequenceNumber, command));
            printWriter.flush();

            System.out.printf(">> Uploading \"%s\" ...\n", fileName);

            bufferedInputStream.read(byteArray, 0, byteArray.length);
            byteArray = Message.appendMessageSequence(++sequenceNumber, byteArray);

            try {
                byteArray = AES.encrypt(byteArray, encryptionKey);
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to create Pi Face Device", e);
            }


            bufferedOutputStream.write(byteArray, 0, byteArray.length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

            System.out.printf(">> Complete uploading \"%s\"\n\n", uploadedFile.getAbsolutePath());
        }
        catch (FileNotFoundException e) {
            System.out.println(">> Invalid file path.");
            System.out.println();

            // keep connection alive
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, "stay"));
            printWriter.flush();
            printWriter.close();
        }
    }


    /***
     * method: requestCertificate
     *
     * request the CA certificate file from the server
     *
     * @throws IOException
     */
    protected void requestCertificate() throws IOException, InvalidMessageException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner serverInput = new Scanner(new InputStreamReader(inputStream));
        String messageReceived = "";
        byte[] byteBlock = new byte[1];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        printWriter.println(Message.appendMessageSequence(sequenceNumber, "Request certificate"));
        printWriter.flush();

        if (!serverInput.hasNextLine()) {
            throw new IOException();
        }

        // confirmation message
        messageReceived = serverInput.nextLine();

        // error
        if (!Message.validateMessageSequenceNumber(++sequenceNumber, messageReceived)) {
            throw new InvalidMessageException();
        }
        else if (!messageReceived.split(DELIMITER)[1].equals("Sending certificate")) {
            throw new InvalidMessageException();
        }
        // valid certificate
        else {
            String certificatePath = src.getAbsolutePath() + "/" + CERTIFICATION;
            File certificate = new File(certificatePath);
            fileOutputStream = new FileOutputStream(certificate);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            int byteRead = inputStream.read(byteBlock, 0, byteBlock.length);

            while (byteRead >= 0) {
                byteArrayOutputStream.write(byteBlock);
                byteRead = inputStream.read(byteBlock);
            }

            if (!Message.validateMessageSequenceNumber(++sequenceNumber, byteArrayOutputStream.toByteArray())) {
                printWriter.close();
                throw new InvalidMessageException();
            }

            // extracted byte array that contains only the file
            byte[] file = Message.extractMessage(byteArrayOutputStream.toByteArray());
            bufferedOutputStream.write(file);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
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
    protected void help() throws IOException {
        System.out.println(">> All commands: ");
        System.out.println("       quit:            end session");
        System.out.println("       list:            list all files of the server");
        System.out.println("       list-me:         list all files of the client");
        System.out.println("       help:            list all possible commands accessible by client");
        System.out.println("       download | file: download the existing \"file\" (name of file only) from the server");
        System.out.println("       upload | file:   upload the \"file\" (absolute path to the file) to the server");
        System.out.println();

        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        printWriter.println(Message.appendMessageSequence(++sequenceNumber, "stay"));
        printWriter.flush();
        printWriter.close();
    }


    /***
     * method: authenticate
     *
     * client authenticates server with keys
     *
     * @return true if success, false if not
     * @throws IOException if socket error
     */
    protected boolean authenticate() throws IOException {
        final boolean AUTHENTICATE_SUCCESS = true;
        final boolean AUTHENTICATE_FAILURE = false;
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner serverInput = new Scanner(new InputStreamReader(inputStream));
        String serverResponse = null;

        if (!hasReceivedCertificate) {
            boolean status = AUTHENTICATE_FAILURE;

            try {
                deleteCertificate();
                requestCertificate();
                if (!verifyCertificate()) {
                    status = AUTHENTICATE_FAILURE;
                }
                else {
                    hasReceivedCertificate = true;
                    status = AUTHENTICATE_SUCCESS;
                }
            }
            catch (IOException e) {
                status = AUTHENTICATE_FAILURE;
            }
            catch (InvalidMessageException e) {
                handleInvalidMessages();
                status = AUTHENTICATE_FAILURE;
            }
            finally {
                deleteCertificate();
            }

            return status;
        }
        else if (!hasSentKey) {
            try {
                String encryptedMasterKey = publicEncrypt(masterKey, serverPublicKey);
                printWriter.println(Message.appendMessageSequence(++sequenceNumber, Long.toString(id)));
                printWriter.println(Message.appendMessageSequence(++sequenceNumber, encryptedMasterKey));

                encryptionKey = AES.modifyKey(masterKey, 1);
                signatureKey = AES.modifyKey(masterKey, 2);
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to encrypt the key", e);
            }

            // confirmation
            if (!serverInput.hasNextLine()) {
                return AUTHENTICATE_FAILURE;
            }

            serverResponse = serverInput.nextLine();

            if (!Message.validateMessageSequenceNumber(++sequenceNumber, serverResponse)) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }
            else if (!serverResponse.split(DELIMITER)[1].equalsIgnoreCase("ok")) {
                return AUTHENTICATE_FAILURE;
            }
            else {
                hasSentKey = true;
            }
        }
        // has received certificate and has sent key
        else {
            // update keys
            encryptionKey = AES.modifyKey(encryptionKey, 2);
            signatureKey = AES.modifyKey(signatureKey, 2);

            printWriter.println(Message.appendMessageSequence(++sequenceNumber, Long.toString(id)));
            printWriter.flush();

            if (!serverInput.hasNextLine()) {
                return AUTHENTICATE_FAILURE;
            }

            serverResponse = serverInput.nextLine();

            // errors
            if (serverResponse == null) {
                return AUTHENTICATE_FAILURE;
            }
            else if (!Message.validateMessageSequenceNumber(++sequenceNumber, serverResponse)) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }
            else if (!serverResponse.split(DELIMITER)[1].equalsIgnoreCase("ok")) {
                return AUTHENTICATE_FAILURE;
            }
        }

        return AUTHENTICATE_SUCCESS;
    }


    /***
     * method: stringToPublicKey
     *
     * Transfer the key from format String to PublicKey
     *
     * @param key: key of format string
     * @return key as PublicKey
     */
    private PublicKey stringToPublicKey(String key) throws Exception {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }


    /***
     * method: verifyCertificate
     *
     * Use the public key to verify the certificate
     *
     //     * @param certPath: the path of certificate path
     * @return true if the verify succeeds, false if not
     */
    protected boolean verifyCertificate() {
        Certificate cert = null;
        PublicKey caPublicKey = null;
        boolean verifySuccess = true;
        FileInputStream in = null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            File certificate = new File(src.getAbsolutePath() + "/" + CERTIFICATION);
            in = new FileInputStream(certificate);
            cert = cf.generateCertificate(in);
            in.close();
            X509Certificate t = (X509Certificate) cert;
            Date timeNow = new Date();
            t.checkValidity(timeNow);
            String publicKey = getKey("CAPublicKey.txt");
            caPublicKey = stringToPublicKey(publicKey);
            cert.verify(caPublicKey);
            serverPublicKey = cert.getPublicKey();
            System.out.println("The Certificate is successfully verified.");
        }
        catch (Exception e) {
            verifySuccess = false;
            System.out.println("The Certificate is not verified.");
            e.printStackTrace();
        }
        finally {
            try {
                in.close();
            }
            catch (Exception e) {
                System.out.println("Cannot close the Certificate.");
            }
        }

        return verifySuccess;
    }


    /***
     * method: deleteCertificate
     *
     * delete the CA certificate out of the system
     * to prevent break-in attack
     */
    protected void deleteCertificate() {
        File certificate = new File(src.getAbsolutePath() + "/" + CERTIFICATION);

        if (certificate.exists() && !certificate.delete()) {
            System.out.println("Cannot delete certificate");
        }
    }


    /***
     * method: publicEncrypt
     *
     * Encrypt the masterkey by server's publickey
     */
    private static String publicEncrypt(String input, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] bt_encrypted = cipher.doFinal(input.getBytes());
        return bytesToString(bt_encrypted);
    }


    /***
     * method: bytesToString
     *
     * One step used in sending key
     */
    private static String bytesToString(byte[] encryptByte) {
        StringBuilder result = new StringBuilder();
        for (Byte bytes : encryptByte) {
            result.append(bytes.toString()).append(" ");
        }
        return result.toString();
    }

}
