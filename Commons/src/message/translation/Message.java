package message.translation;

import message.translation.Cryptor;
import utils.Printable;

import utils.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;


public class Message implements Printable {

    private static final int MINIMUM_MESSAGE_LENGTH = 70;

    public static final String END_OF_MESSAGE_DELIMITER_STRING = "<EndOfMessage/>";
    public static final byte[] END_OF_MESSAGE_DELIMITER_BYTES = END_OF_MESSAGE_DELIMITER_STRING.getBytes(Cryptor
            .CHARSET);


    /***
     * method: appendMessageSequence
     *
     * add the message sequence to the front of the message
     *
     * @param sequence: message sequence number
     * @param message: the message
     * @return new message with its sequence number
     */
    public static String appendMessageSequence(int sequence, String message) {
        String newMessage = String.format("%d | %s", sequence, message);

        if (Printable.IS_PRINTABLE) {
            displayMessage(newMessage);
        }

        return newMessage;
    }


    /***
     * method: appendMessageSequence
     *
     * add the message sequence to the front of the message
     *
     * @param sequence: message sequence number
     * @param message: the message (as byte array)
     * @return new message with its sequence number
     */
    public static byte[] appendMessageSequence(int sequence, byte[] message) {
        byte[] newMessage = new byte[message.length + 7];
        byte[] intAsByteArray = ByteBuffer.allocate(4).putInt(sequence).array();

        // message sequence
        System.arraycopy(intAsByteArray, 0, newMessage, 0, 4);
        newMessage[4] = ' ';
        newMessage[5] = '|';
        newMessage[6] = ' ';

        // original message
        System.arraycopy(message, 0, newMessage, 7, message.length);

        if (Printable.IS_PRINTABLE) {
            displayMessage(newMessage);
        }

        return newMessage;
    }


    private static String increaseMessageLength(String messageWithSequence) {
        int numberOfAdditionalCharactersRequired = MINIMUM_MESSAGE_LENGTH - messageWithSequence.length();

        if(numberOfAdditionalCharactersRequired <= 0) {
            return messageWithSequence;
        }
        else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(messageWithSequence);
            stringBuilder.append(END_OF_MESSAGE_DELIMITER_STRING);

            for(int i = 0; i < numberOfAdditionalCharactersRequired; i++) {
                int randomIndex = (int)(Math.random() * Cryptor.DEFAULT_LANGUAGE.length());
                stringBuilder.append(Cryptor.DEFAULT_LANGUAGE.charAt(randomIndex));
            }

            return stringBuilder.toString();
        }
    }


    private static byte[] increaseMessageLength(byte[] messageWithSequence) {
        int numberOfAdditionalCharactersRequired = MINIMUM_MESSAGE_LENGTH - messageWithSequence.length;

        if(numberOfAdditionalCharactersRequired <= 0) {
            return messageWithSequence;
        }
        else {
            byte[] newMessage = new byte[
                    messageWithSequence.length + END_OF_MESSAGE_DELIMITER_BYTES.length +
                    numberOfAdditionalCharactersRequired];
            int currentIndex = 0;

            for(byte b : messageWithSequence) {
                newMessage[currentIndex++] = b;
            }

            for(byte b : END_OF_MESSAGE_DELIMITER_BYTES) {
                newMessage[currentIndex++] = b;
            }

            for(int i = 0; i < numberOfAdditionalCharactersRequired; i++) {
                int randomIndex = (int)(Math.random() * Cryptor.DEFAULT_LANGUAGE.length());
                newMessage[currentIndex++] = (byte) Cryptor.DEFAULT_LANGUAGE.charAt(randomIndex);
            }

            return newMessage;
        }
    }


    /***
     * method: validateMessageSequenceNumber
     *
     * validate if the message is the expected one
     *
     * @param message: the message
     * @param expectedSequenceNumber: expected sequence to be
     * @return true if correct sequence number, false otherwise
     */
    public static boolean validateMessageSequenceNumber(int expectedSequenceNumber, String message) {
        boolean isValidSequence = false;
        int actualSequenceNumber = 0;
        String seq = null;

        if (Printable.IS_PRINTABLE) {
            displayMessage(message);
        }

        if (!message.contains(" ")) {
            return false;
        }

        try {
            seq = message.substring(0, message.indexOf(" "));
            actualSequenceNumber = Integer.parseInt(seq);
            isValidSequence = actualSequenceNumber == expectedSequenceNumber;
        }
        catch (NumberFormatException e) {
            isValidSequence = false;
        }

        return isValidSequence;
    }


    /***
     * method: validateMessageSequenceNumber
     *
     * validate if the message is the expected one
     *
     * @param message: the message
     * @param expectedSequenceNumber: expected sequence to be
     * @return true if correct sequence number, false otherwise
     */
    public static boolean validateMessageSequenceNumber(int expectedSequenceNumber, byte[] message) {
        boolean isValidSequence = false;
        int actualSequenceNumber = 0;
        byte[] sequenceNumberAsBytes = new byte[4];
        ByteBuffer byteBuffer = null;

        // first 7 bytes are for sequence and delimiter -> length has to be at least 8
        if (message.length <= 7) {
            return false;
        }

        // extract first 4 bytes as sequence number
        System.arraycopy(message, 0, sequenceNumberAsBytes, 0, 4);
        byteBuffer = ByteBuffer.wrap(sequenceNumberAsBytes);

        // check
        try {
            actualSequenceNumber = byteBuffer.getInt();
            isValidSequence = actualSequenceNumber == expectedSequenceNumber;
        }
        catch (BufferUnderflowException e) {
            isValidSequence = false;
        }

        return isValidSequence;
    }


    /***
     * method: extractMessage
     *
     * remove the first 7 bytes (sequence number and delimiter) out of the message
     *
     * @param messageWithSequence: message with sequence number in front
     * @return the actual message as byte array without the sequence number and delimiter
     */
    public static byte[] extractMessage(byte[] messageWithSequence) {
        if (Printable.IS_PRINTABLE) {
            displayMessage(messageWithSequence);
        }

        byte[] extractedMessage = new byte[messageWithSequence.length - 7];
        System.arraycopy(messageWithSequence, 7, extractedMessage, 0, extractedMessage.length);
        return extractedMessage;
    }


    /***
     * method: displayMessage
     *
     * print out the string message
     *
     * @param message: message as string
     */
    private static void displayMessage(String message) {
        System.out.println(message);
    }


    /***
     * method: displayMessage
     *
     * print out the sequence number and the delimiter, following by ...
     * from the message that is in its byte array form
     *
     * @param message: message as byte array
     */
    private static void displayMessage(byte[] message) {
        ByteBuffer byteBuffer = null;
        byte[] integer = new byte[4];

        for (int i = 0; i < 8; i++) {
            if (i < 4) {
                integer[i] = message[i];
            }
            else if (i == 4) {
                byteBuffer = ByteBuffer.wrap(integer);
                System.out.print(byteBuffer.getInt());
                System.out.printf("%c", message[i]);
            }
            else if (i <= 6) {
                System.out.printf("%c", message[i]);
            }
            else {
                System.out.print("...");
            }
        }
        System.out.println();
    }

}
