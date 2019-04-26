package app;

import util.Cryptor;
import util.Printable;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public abstract class Peer implements Printable {
    private String module = null;
    protected Socket clientSocket = null;
    protected File filesDirectory = null;
//    protected File src = null;
    protected File keysDirectory = null;
    protected Cryptor cryptor = null;
    protected String masterKey = null;
    protected String encryptionKey = null;
    protected String signatureKey = null;
    protected DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    protected Date date = null;
    protected int sequenceNumber = 0;
    protected int totalInvalidMessagesReceived = 0;
    protected static final int MAX_INVALID_MESSAGES_ALLOWED = 5;
    protected final String DELIMITER = "\\s+\\|\\s+";
    protected final String BIG_DIV = "\n======================================================\n";
    protected final String SMALL_DIV = "\n---------------------\n";
    protected static final String SERVER = "Server";
    protected static final String CLIENT = "Client";
    protected static final int TIME_OUT = (int) TimeUnit.MINUTES.toMillis(5);


    public Peer(String module) {
        this.module = module;
    }


    /***
     * method: notifyConnectionSuccess
     *
     * if successfully connect for the first time:
     *      set initial connection start time
     *      from now is busy (not initial session anymore)
     *
     * @param initialSession: first successful connection
     *
     * @return not initial connection
     */
    protected boolean notifyConnectionSuccess(boolean initialSession) {
        if (!initialSession) {
            date = new Date();
            System.out.println("Connection established at " + dateFormat.format(date));
            System.out.println(SMALL_DIV);
        }
        return true;
    }


    /***
     * method: notifyConnectionEnd
     *
     * display the conclusion of the connection
     *
     * @param isConnectionSuccess: is the connection actually established between Client and Server?
     */
    protected void notifyConnectionEnd(boolean isConnectionSuccess) {
        System.out.println(SMALL_DIV);
        date = new Date();
        System.out.printf("Connection %s at %s\n",
                isConnectionSuccess ? "ended" : "failed",
                dateFormat.format(date));
        System.out.println(BIG_DIV);
    }


    /***
     * method: isIntruderDetected
     *
     * intruder is detected if the total invalid messages received
     * is more than the allowed threshold
     *
     * @return true if detect intruder, false otherwise
     */
    protected boolean isIntruderDetected() {
        return totalInvalidMessagesReceived > MAX_INVALID_MESSAGES_ALLOWED;
    }


    /***
     * method: setDirectories
     *
     * set the Files Directory to store all files
     * remove the "\src" in the path when run from the command line environment
     */
    protected void setDirectories() {
        // file directory
        filesDirectory = new File(module + "/src/main/resources/FilesDirectory");
        if(!Files.exists(Paths.get(filesDirectory.getAbsolutePath())))
        {
            filesDirectory = new File("../src/main/resources/FilesDirectory");
        }

        // keys directory
        keysDirectory = new File(module + "/src/main/resources/Keys");
        if(!Files.exists(Paths.get(keysDirectory.getAbsolutePath())))
        {
            keysDirectory = new File("../src/main/resources/Keys");
        }

    }


    /***
     * method: handleInvalidMessages
     *
     * handle the invalid message
     */
    protected void handleInvalidMessages() {
        handleInvalidMessages(true);
    }


    /***
     * method: handleInvalidMessages
     *
     * increase the total invalid messages received count
     *
     * if the message is string (just commands):
     *      decrease the sequence number
     *      decrease cryptor.offset to rollback
     * if the message is a file:
     *       do not adjust anything
     */
    protected void handleInvalidMessages(boolean isInvalidString) {
        totalInvalidMessagesReceived++;
        if(isInvalidString) {
            sequenceNumber--;
            cryptor.adjustOffset(-1);
        }
    }


    /***
     * method: validateMAC
     *
     * validate MAC code in the byte array
     *
     * @param byteStream: file with MAC as byte array
     * @return true if valid MAC, false otherwise
     */
    protected boolean validateMAC(byte[] byteStream) {
        byte[] byteArray = new byte[byteStream.length - 20 + signatureKey.length()];
        byte[] mac = new byte[20];

        System.arraycopy(byteStream, 0, mac, 0, 20);
        System.arraycopy(signatureKey.getBytes(), 0, byteArray, 0, signatureKey.length());
        System.arraycopy(byteStream, 20, byteArray, signatureKey.length(), byteStream.length - 20);

        return Arrays.equals(mac, cryptor.sha1(new String(byteArray, Cryptor.CHARSET)));
    }


    /***
     * method: getKey
     *
     * read the key from the local file and return as string
     *
     * @param keyFileName: local file that contains the key
     * @return key as string
     */
    protected String getKey(String keyFileName) {

        File file = new File(String.format("%s/src/main/resources/Keys/%s", module, keyFileName));

        if (!file.exists()) {
            String path = String.format("../src/main/resources/Keys/%s",  keyFileName);
            file = new File(path);
        }

        StringBuilder key = new StringBuilder();

        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
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
     * method: displayPeerMessage
     *
     * print the main.java.peer's message/command to the screen
     * without the message sequence
     *
     * @param message: main.java.peer's message
     */
    protected void displayPeerMessage(String message) {
        String[] messageTokens = message.split(DELIMITER);

        System.out.printf("[%s]: ", module.equals(SERVER) ? CLIENT : SERVER);

        for (int i = 1; i < messageTokens.length; i++) {
            System.out.print(messageTokens[i]);

            if (i != messageTokens.length - 1) {
                System.out.print(" | ");
            }
        }
        System.out.println();
    }


    /***
     * method: printKeys
     *
     * display all keys to the screen
     */
    protected void printKeys() {
        if (IS_PRINTABLE) {
            System.out.println("\n<!--");
            System.out.println("   MasterKey      : " + masterKey);
            System.out.println("   Encryption     : " + encryptionKey);
            System.out.println("   Signature      : " + signatureKey);
            System.out.println("   SequenceNumber : " + sequenceNumber);
            System.out.println("-->\n");
        }
    }

}
