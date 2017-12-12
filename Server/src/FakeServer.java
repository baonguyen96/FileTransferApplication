import java.io.*;
import java.util.Scanner;


public class FakeServer extends Server implements Resynchronizable {
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
        if (!receivedInput.hasNextLine()) {
            System.out.println(">> Client is offline.");
            return STOP_CONNECTION_AFTER_THIS;
        }

        receivedCommand = receivedInput.nextLine();
        receivedCommand = aes.decrypt(receivedCommand);

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

        // valid command excluding "stay"
        commandTokens = receivedCommand.split(DELIMITER);
        displayPeerMessage(receivedCommand);

        // isAbleToMessUpSynchronization is opposite of IS_RESYNCHRONIZABLE
        if (isAbleToMessUpSynchronization) {
            sequenceNumber--;
            isAbleToMessUpSynchronization = !IS_RESYNCHRONIZABLE;
            aes.adjustOffset(-1);
        }

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
        /*
         * else == stay
         *      don't do anything
         */


        return CONTINUE_CONNECTION_AFTER_THIS;
    }

}

