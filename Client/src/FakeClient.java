import java.io.*;
import java.util.Scanner;


public class FakeClient extends Client implements Resynchronizable {
    private boolean isAbleToMessUpSynchronization = true;
    private final boolean IS_ABLE_TO_MESS_UP_AUTHENTICATION = true;


    private FakeClient() {
        super();
    }


    public static void main(String[] args) {
        FakeClient fakeClient = new FakeClient();
        fakeClient.exec();
    }


    /***
     * method: communicate
     *
     * communication session between server and client
     *
     * @return true if stop communication, false if not
     * @throws IOException
     */
    @Override
    protected final boolean communicate() throws IOException {
        String command = getCommand();
        String[] commandComponents = command.split(DELIMITER);
        final boolean STOP_CONNECTION_AFTER_THIS = true;
        final boolean CONTINUE_CONNECTION_AFTER_THIS = false;

        // isAbleToMessUpSynchronization is opposite of IS_RESYNCHRONIZABLE
        if(isAbleToMessUpSynchronization) {
            sequenceNumber--;
            isAbleToMessUpSynchronization = !IS_RESYNCHRONIZABLE;
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
    
}
