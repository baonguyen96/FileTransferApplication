import java.io.File;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public abstract class Peer {

    protected Socket clientSocket = null;
    protected File filesDirectory = null;
    protected File src = null;
    protected String masterKey = null;
    private String module = null;
    protected DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    protected Date date = null;
//	protected String masterKey2 = null;    // why is this?
    protected int sequenceNumber = 0;
    protected int totalInvalidMessagesReceived = 0;
    protected static final int MAX_INVALID_MESSAGES_ALLOWED = 5;
    protected final String DELIMITER = "\\s+\\|\\s+";
    protected final String BIG_DIV = "\n======================================================\n";
    protected final String SMALL_DIV = "\n---------------------\n";
    protected static final String SERVER = "Server";
    protected static final String CLIENT = "Client";


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
        if(!initialSession) {
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
        filesDirectory = new File( module + "/FilesDirectory");
        String absolutePath = filesDirectory.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
        absolutePath = absolutePath.replace("/src", "");
        absolutePath = absolutePath.replace(
                String.format("/%s/%s", module, module),
                String.format("/%s", module)
        );
        filesDirectory = new File(absolutePath);

        src = new File(module + "/src");
        absolutePath = src.getAbsolutePath();
        absolutePath = absolutePath.replace("\\", "/");
        absolutePath = absolutePath.replace(
                String.format("/%s/src/%s/src", module, module),
                String.format("/%s/src", module)
        );
        src = new File(absolutePath);
    }



    /***
     * method: handleInvalidMessages
     *
     * increase the total invalid messages received count
     * decrease the sequence number to rollback
     */
    protected void handleInvalidMessages() {
        totalInvalidMessagesReceived++;
        sequenceNumber--;
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
		
        File file = new File(keyFileName);
        if(!file.exists()) {
            String path = String.format("%s/src/%s", module, keyFileName);
            file = new File(path);
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
     * method: displayPeerMessage
     *
     * print the peer's message/command to the screen
     * without the message sequence
     *
     * @param message: peer's message
     */
    protected void displayPeerMessage(String message) {
        String[] messageTokens = message.split(DELIMITER);

        System.out.printf("[%s]: ", module.equals(SERVER) ? CLIENT : SERVER);

        for(int i = 1; i < messageTokens.length; i++) {
            System.out.print(messageTokens[i]);

            if(i != messageTokens.length - 1) {
                System.out.print(" | ");
            }
        }
        System.out.println();
    }

}
