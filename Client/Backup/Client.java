import java.io.*;
import java.net.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import sun.misc.BASE64Decoder;


public class Client {
    private Socket clientSocket = null;
    private File fileDirectory = null;
    private File src = null;
    private final long id = System.currentTimeMillis();
    private boolean connectSuccess = false;
    private boolean hasReceivedCertificate = false;
    private boolean hasSentKey = false;
    private long masterKey = 99;
    private final String CERTIFICATION = "CA-certificate.crt";


    private Client() {
    }


    public static void main(String[] args) {
        Client client = new Client();
        client.exec();
    }


    private void exec() {
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

            while (!stopCommunication) {
                // create a socket to connect to the server on port 1111
                clientSocket = new Socket(serverIPAddress, 1111);
                clientSocket.setSoTimeout(30000);


                // authentication
                if (!authenticate()) {
                    if(!connectSuccess) {
                        System.out.println("\nAccess denied.");
                        clientSocket.close();
                        break;
                    }
                    else {
                        System.out.println("communicate");
                        stopCommunication = communicate();
                        clientSocket.close();
                        continue;
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
            System.out.println("\nError: unknown IP address.");
        }
        catch (SocketTimeoutException e) {
            System.out.println("\nError: server is busy.");
        }
        catch (FileNotFoundException e) {
            System.out.println("\nError: cannot create or find file.");
        }
        catch (IOException e) {
            System.out.println("\nError: sockets corrupted.");
        }
//        catch (Exception e) {
//            System.out.println("\nError: certificate.");
//        }
        finally {
            deleteCertificate();
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
    private boolean isValidCommand(String clientCommand, String delimiter) {
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
    private boolean communicate() throws IOException {
        final String DELIMITER = "\\s+\\|\\s+";
        String command = getCommand(DELIMITER);
        String[] commandComponents = command.split(DELIMITER);
        final boolean STOP_CONNECTION_AFTER_THIS = true;

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
    private String getCommand(String delimiter) {
        Scanner input = new Scanner(System.in);
        String command = "";
        boolean isValid = false;

        do {
            System.out.print("[You]:    ");
            command = input.nextLine();

            if (!isValidCommand(command, delimiter)) {
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
    private void list(String command) throws IOException {
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
    private void listMe() throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        File[] files = fileDirectory.listFiles();
        StringBuilder listOfFiles = new StringBuilder();

        System.out.print(">> ");

        if (files == null) {
            System.out.println("Error: cannot find files fileDirectory.");
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

        printWriter.println(command);
        printWriter.flush();

        // confirmation message
        messageReceived = serverInput.nextLine();

        // error
        if (!messageReceived.contains(fileToDownloadName)) {
            System.out.println("\n[Server]: " + messageReceived);
        }
        // valid file to download
        else {
            fileToDownloadName = fileDirectory.getAbsolutePath() + "/" + fileToDownloadName;
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


    private void requestCertificate() throws IOException {

        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner serverInput = new Scanner(new InputStreamReader(inputStream));
        String messageReceived = "";
        byte[] byteBlock = new byte[1];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        printWriter.println("requestCertificate");
        printWriter.flush();

        if (!serverInput.hasNextLine()) {
            throw new IOException();
        }

        // confirmation message
        messageReceived = serverInput.nextLine();

        // error
        if(!messageReceived.equals("sending certificate")) {
            throw new IOException();
        }
        // valid certificate
        else {
            File certificate = new File(src.getAbsolutePath() + "/" + CERTIFICATION);
            fileOutputStream = new FileOutputStream(certificate);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            int byteRead = inputStream.read(byteBlock, 0, byteBlock.length);

            while (byteRead >= 0) {
                byteArrayOutputStream.write(byteBlock);
                byteRead = inputStream.read(byteBlock);
            }

            bufferedOutputStream.write(byteArrayOutputStream.toByteArray());
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
    private void setDirectory() {
        fileDirectory = new File("Client/FilesDirectory");
        String absolutePath = fileDirectory.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
        absolutePath = absolutePath.replace("/src", "");
        absolutePath = absolutePath.replace("/Client/Client", "/Client");
        fileDirectory = new File(absolutePath);

        src = new File("Client/src");
        absolutePath = src.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
//        absolutePath = absolutePath.replace("/src", "");
        absolutePath = absolutePath.replace("/Client/Client", "/Client");
        src = new File(absolutePath);
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

        /*
         * this is a prototype to authenticate based on the same initial IP address and ID
         * need to expand the protocol to cover keys exchange and encryption/decryption
         * see Server.authenticate() for more information
         */
        final boolean AUTHENTICATE_SUCCESS = true;
        final boolean AUTHENTICATE_FAILURE = false;
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        InputStream inputStream = clientSocket.getInputStream();
        Scanner serverInput = new Scanner(new InputStreamReader(inputStream));

        if(!hasReceivedCertificate) {
            try {
                requestCertificate();
                if (!verifyCertificate()) {
                    System.out.println("certificate denied.");
                    return AUTHENTICATE_FAILURE;
                }
                hasReceivedCertificate = true;

            }
            catch (IOException e) {
                return AUTHENTICATE_FAILURE;
            }
        }
        else if(!hasSentKey) {
            printWriter.println(id);
            printWriter.println(masterKey);
            hasSentKey = true;
        }
        else {

            printWriter.println(id);
//            printWriter.flush();

            if (!serverInput.hasNextLine()) {
                return AUTHENTICATE_FAILURE;
            }

            String serverResponse = serverInput.nextLine();
            if (serverResponse == null || !serverResponse.equalsIgnoreCase("ok")) {
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
     * method: Verify
     *
     * Use the public key to verify the certificate
     *
     //     * @param certPath: the path of certificate path
     * @return true if the verify succeeds, false if not
     */

    private boolean verifyCertificate() {
        Certificate cert;
        PublicKey caPublicKey;
        boolean verifysuccess = true;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream in = new FileInputStream(src.getAbsolutePath() + "/" + CERTIFICATION);
            cert = cf.generateCertificate(in);
            in.close();
            X509Certificate t = (X509Certificate) cert;
            Date timeNow = new Date();
            t.checkValidity(timeNow);
            String publicKey = getKey("CAPublicKey.txt");
            caPublicKey = getPublicKey(publicKey);
            try {
                cert.verify(caPublicKey);
                System.out.println("The Ceritificate is verified.\n");
            }
            catch (Exception e) {
                verifysuccess = false;
                System.out.println("The verify is not pass.\n");
                e.printStackTrace();
            }

        }
        catch (CertificateExpiredException e1) {
            e1.printStackTrace();
        }
        catch (CertificateNotYetValidException e1) {
            e1.printStackTrace();
        }
        catch (CertificateException e1) {
            e1.printStackTrace();
        }
        catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return verifysuccess;
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
            file = new File("Client/src/" + keyFileName);
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


    private void deleteCertificate() {
        File certificate = new File(src.getAbsolutePath() + "/" + CERTIFICATION);

        if(certificate.exists()) {
            certificate.delete();
        }
    }

}