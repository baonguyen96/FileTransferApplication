import message.translation.Cryptor;
import message.translation.Message;
import utils.InvalidMessageException;
import utils.Resynchronizable;

import java.io.*;
import java.net.Socket;


public class FakeClient extends Client implements Resynchronizable {
    private boolean isAbleToMessUpSynchronization = true;
    private final boolean IS_ABLE_TO_MESS_UP_AUTHENTICATION = false;
    private final boolean IS_BYPASSING_AUTHENTICATION_TO_FAKE_UPLOAD = true;


    protected FakeClient() {
        super();
    }


    public static void main(String[] args) {
        FakeClient fakeClient = new FakeClient();
        fakeClient.exec();
    }


    /***
     * method: exec
     *
     * execute the Client and control the flow of the program
     * if bypassing authentication:
     *      simply send the upload command and the file
     */
    @Override
    protected void exec() {
        if(!IS_BYPASSING_AUTHENTICATION_TO_FAKE_UPLOAD) {
            super.exec();
        }
        else {
            try {
                clientSocket = new Socket("localhost", 1111);
                cryptor = new Cryptor();
                OutputStream outputStream = clientSocket.getOutputStream();
                PrintWriter printWriter = new PrintWriter(outputStream);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                String fileName = new File("").getAbsolutePath();
                fileName = fileName.concat("\\Assets\\Test Upload\\fake.txt");
                File fileToSend = new File(fileName);
                byte[] byteArray = new byte[(int) fileToSend.length()];
                byte[] byteArray2 = new byte[byteArray.length + 20 + 7];
                byte[] temp = new byte[20 + byteArray.length + 7];
                FileInputStream fileInputStream = new FileInputStream(fileToSend);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                sequenceNumber = 478;     // set sequence number here
                signatureKey = "randomSignature";
                encryptionKey = "randomEncryption";

                // confirmation message
                String confirmationMessage = String.format("Sending \"%s\" ...", fileName);
                System.out.println(">> " + confirmationMessage);
                confirmationMessage = Message.appendMessageSequence(++sequenceNumber, confirmationMessage);    // replace 1 with the actual ongoing sequence
                confirmationMessage = cryptor.encrypt(confirmationMessage);
                printWriter.println(confirmationMessage);
                printWriter.flush();

                // file transfer
                bufferedInputStream.read(byteArray, 0, byteArray.length);
                byteArray = Message.appendMessageSequence(++sequenceNumber, byteArray);
                byteArray = cryptor.encrypt(byteArray, encryptionKey);
                System.arraycopy(signatureKey.getBytes(), 0, temp, 0, signatureKey.length());
                System.arraycopy(byteArray, 0, temp, signatureKey.length(), byteArray.length);

                // append mac
                byte[] mac = cryptor.sha1(new String(temp, Cryptor.CHARSET));
                System.arraycopy(mac, 0, byteArray2, 0, mac.length);
                System.arraycopy(byteArray, 0, byteArray2, 20, byteArray.length);
                bufferedOutputStream.write(byteArray2, 0, byteArray2.length);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();

                System.out.printf(">> Complete sending \"%s\"\n\n", fileName);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        if (isAbleToMessUpSynchronization) {
            sequenceNumber--;
            isAbleToMessUpSynchronization = !IS_RESYNCHRONIZABLE;
            cryptor.adjustOffset(-1);
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
        /*
         * suppose to send encrypted info here, but do not send anything
         * -> keep the server waiting (DoS)
         */
        return AUTHENTICATE_SUCCESS;
    }

}
