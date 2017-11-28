import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class FakeServer extends Server {
    private final boolean IS_RESYNCABLE = false;
    private boolean isAbleToMessUpSynchronization = true;


    private FakeServer() {
        super();
    }


    public static void main(String[] args) {
        FakeServer fakeServer = new FakeServer();
        fakeServer.exec();
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
    @Override
    protected final boolean communicate() throws IOException {
        String receivedCommand = "";
        String[] commandTokens = null;
        final boolean STOP_CONNECTION_AFTER_THIS = true;
        final boolean CONTINUE_CONNECTION_AFTER_THIS = false;
        InputStream inputStream = clientSocket.getInputStream();
        Scanner receivedInput = new Scanner(new InputStreamReader(inputStream));

        // client is offline
        if(!receivedInput.hasNextLine()) {
            System.out.println(">> Client is offline.");
            return STOP_CONNECTION_AFTER_THIS;
        }

        receivedCommand = receivedInput.nextLine();

        // errors
        if(!Message.validateMessageSequenceNumber(++sequenceNumber, receivedCommand)) {
            handleInvalidMessages();
            return CONTINUE_CONNECTION_AFTER_THIS;
        }
        else if(!isValidCommand(receivedCommand)) {
            return CONTINUE_CONNECTION_AFTER_THIS;
        }
        else if(receivedCommand.contains("stay")) {
            return CONTINUE_CONNECTION_AFTER_THIS;
        }

        // valid command
        commandTokens = receivedCommand.split(DELIMITER);
        displayPeerMessage(receivedCommand);

        // isAbleToMessUpSynchronization is opposite of IS_RESYNCABLE
        if(isAbleToMessUpSynchronization) {
            sequenceNumber--;
            isAbleToMessUpSynchronization = !IS_RESYNCABLE;
        }

        // switch
        if(commandTokens[1].equalsIgnoreCase("quit")) {
            return STOP_CONNECTION_AFTER_THIS;
        }
        else if(commandTokens[1].equalsIgnoreCase("list")) {
            list();
        }
        else if(commandTokens[1].equalsIgnoreCase("download")) {
            clientDownload(commandTokens);
        }
        else if(commandTokens[1].equalsIgnoreCase("upload")){
            clientUpload(commandTokens);
        }
        else if(commandTokens[1].equalsIgnoreCase("stay")){
            // stay -> don't do anything
        }


        return CONTINUE_CONNECTION_AFTER_THIS;
    }

}

