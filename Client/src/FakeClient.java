import java.io.IOException;


public class FakeClient extends Client implements Resynchronizable {
    private boolean isAbleToMessUpSynchronization = true;
    private final boolean IS_ABLE_TO_MESS_UP_AUTHENTICATION = false;


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
     * modify the message sequence to simulate intruder
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
            aes.adjustOffset(-1);
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
     * method: authenticate
     *
     * client authenticates server with keys
     * can request certificate and does not send the keys to keep the server waiting
     * -> simulate SYN flood - DDoS attack
     *
     * @return true if success, false if not
     * @throws IOException if socket error
     */
    @Override
    protected boolean authenticate() throws IOException {
        if (!IS_ABLE_TO_MESS_UP_AUTHENTICATION) {
            return super.authenticate();
        }

        final boolean AUTHENTICATE_SUCCESS = true;
        final boolean AUTHENTICATE_FAILURE = false;

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
        else {
            // do not send anything -> keep the server waiting
        }
        return AUTHENTICATE_SUCCESS;
    }

}
