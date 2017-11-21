/*
 * this is internal API and may be removed in the future java release which will break our codes
 * use this instead: https://stackoverflow.com/questions/36578625/base64encoder-is-internal-api-and-may-be-removed-in-future-release
 */
import sun.misc.BASE64Decoder;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;


public class FakeClient extends Peer {
    private final long id = System.currentTimeMillis();
    private boolean connectSuccess = false;
    private boolean hasReceivedCertificate = false;
    private boolean hasSentKey = false;
    private final String CERTIFICATION = "CA-certificate.crt";
    private final boolean IS_RESYNCABLE = true;
    private boolean isAbleToMessUpSynchronization = true;


    private FakeClient() {
        super(CLIENT);
        masterKey = (long) (Math.random() * Long.MAX_VALUE);
    }


    public static void main(String[] args) {
        FakeClient client = new FakeClient();
        client.exec();
    }


    /***
     * method: exec
     *
     * execute the Client and control the flow of the program
     */
    private void exec() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = null;
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

                // end if detect intruder
                if(isIntruderDetected()) {
                    System.out.print(SMALL_DIV);
                    System.out.println("Warning: Intruder detected. Abort connection.");
                    break;
                }

                clientSocket = new Socket(serverIPAddress, 1111);
                clientSocket.setSoTimeout(30000);

                // authentication
                if (!authenticate()) {
                    if(!connectSuccess) {
                        System.out.println(SMALL_DIV);
                        System.out.println("Access denied.");
                        clientSocket.close();
                        break;
                    }

                }
                else if(hasReceivedCertificate && !hasSentKey) {
                    clientSocket.close();
                    continue;
                }


                if (!connectSuccess) {
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
    private boolean communicate() throws IOException {
        String command = getCommand();
        String[] commandComponents = command.split(DELIMITER);
        final boolean STOP_CONNECTION_AFTER_THIS = true;
        final boolean CONTINUE_CONNECTION_AFTER_THIS = false;

        // isAbleToMessUpSynchronization is opposite of IS_RESYNCABLE
        if(isAbleToMessUpSynchronization) {
            sequenceNumber--;
            isAbleToMessUpSynchronization = !IS_RESYNCABLE;
        }

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
    private String getCommand() {
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
    private void quit(String command) throws IOException {
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
    private void list(String command) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        Scanner serverInput = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
        String messageReceived = "";

        printWriter.println(Message.appendMessageSequence(++sequenceNumber, command));
        printWriter.flush();

        // lost message -> may have to do something about it
        if(!serverInput.hasNextLine()) {
            printWriter.close();
            return;
        }

        messageReceived = serverInput.nextLine();

        if(!Message.validateMessageSequenceNumber(++sequenceNumber, messageReceived)) {
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
    private void listMe() throws IOException {
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
    private void download(String command, String[] commandComponents) throws IOException {
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
        if(!serverInput.hasNextLine()) {
            return;
        }

        // confirmation message
        messageReceived = serverInput.nextLine();

        // errors on confirmation
        if(!Message.validateMessageSequenceNumber(++sequenceNumber, messageReceived)) {
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

            // validate sequence number
            if(!Message.validateMessageSequenceNumber(++sequenceNumber, byteStream)) {
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
    private void upload(String command, String[] commandComponents) throws IOException {
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
    private void requestCertificate() throws IOException, InvalidMessageException {
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
        if(!Message.validateMessageSequenceNumber(++sequenceNumber, messageReceived)) {
            throw new InvalidMessageException();
        }
        else if(!messageReceived.split(DELIMITER)[1].equals("Sending certificate")) {
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

            if(!Message.validateMessageSequenceNumber(++sequenceNumber, byteArrayOutputStream.toByteArray())) {
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
    private void help() throws IOException {
        System.out.println(">> All commands: ");
        System.out.println("       quit:            end session");
        System.out.println("       list:            list all files of the server");
        System.out.println("       list-me:         list all files of the client");
        System.out.println("       help:            list all possible commands accessible by client");
        System.out.println("       download | file: download the existing \"file\" from the server");
        System.out.println("       upload | file:   upload the \"file\" (absolute path) to the server");
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
    private boolean authenticate() throws IOException {
        final boolean AUTHENTICATE_SUCCESS = true;
        final boolean AUTHENTICATE_FAILURE = false;
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner serverInput = new Scanner(new InputStreamReader(inputStream));
        String serverResponse = null;

        if(!hasReceivedCertificate) {
            try {
                deleteCertificate();
                requestCertificate();
                if (!verifyCertificate()) {
                    System.out.println("Error: Certificate denied.");
                    return AUTHENTICATE_FAILURE;
                }
                hasReceivedCertificate = true;

            }
            catch (IOException e) {
                return AUTHENTICATE_FAILURE;
            }
            catch (InvalidMessageException e) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }
            finally {
                deleteCertificate();
            }
        }
        else if(!hasSentKey) {
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, Long.toString(id)));
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, Long.toString(masterKey)));

            // confirmation
            if(!serverInput.hasNextLine()) {
                return AUTHENTICATE_FAILURE;
            }

            serverResponse = serverInput.nextLine();

            if(!Message.validateMessageSequenceNumber(++sequenceNumber, serverResponse)) {
                handleInvalidMessages();
                return AUTHENTICATE_FAILURE;
            }
            else if(!serverResponse.split(DELIMITER)[1].equalsIgnoreCase("ok")) {
                return AUTHENTICATE_FAILURE;
            }
            else {
                hasSentKey = true;
            }
        }
        // has received certificate and has sent key
        else {
            printWriter.println(Message.appendMessageSequence(++sequenceNumber, Long.toString(id)));
            printWriter.flush();

            if (!serverInput.hasNextLine()) {
                return AUTHENTICATE_FAILURE;
            }

            serverResponse = serverInput.nextLine();

            // errors
            if(serverResponse == null) {
                return AUTHENTICATE_FAILURE;
            }
            else if(!Message.validateMessageSequenceNumber(++sequenceNumber, serverResponse)) {
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
     * method: getPublicKey
     *
     * Transfer the key from format String to PublicKey
     *
     * @param key: key of format string
     * @return key as PublicKey
     */
    private PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes;
        keyBytes = (new BASE64Decoder()).decodeBuffer(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }


    /***
     * method: verifyCertificate
     *
     * Use the public key to verify the certificate
     *
     //     * @param certPath: the path of certificate path
     * @return true if the verify succeeds, false if not
     */
    private boolean verifyCertificate() {
        Certificate cert;
        PublicKey caPublicKey;
        boolean verifySuccess = true;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            File certificate = new File(src.getAbsolutePath() + "/" + CERTIFICATION);
            FileInputStream in = new FileInputStream(certificate);
            cert = cf.generateCertificate(in);
            in.close();
            X509Certificate t = (X509Certificate) cert;
            Date timeNow = new Date();
            t.checkValidity(timeNow);
            String publicKey = getKey("CAPublicKey.txt");
            caPublicKey = getPublicKey(publicKey);
            cert.verify(caPublicKey);
            System.out.println("The Certificate is successfully verified.");
        }
        catch (Exception e) {
            verifySuccess = false;
            System.out.println("The Certificate is not verified.");
            e.printStackTrace();
        }
        return verifySuccess;
    }


    /***
     * method: deleteCertificate
     *
     * delete the CA certificate out of the system
     * to prevent break-in attack
     */
    private void deleteCertificate() {
        File certificate = new File(src.getAbsolutePath() + "/" + CERTIFICATION);
        if(certificate.exists()) {
            certificate.delete();
        }
    }

}