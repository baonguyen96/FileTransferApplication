public class Message {

    /***
     * method: validateMessageSequenceNumber
     *
     * validate if the message is the expected one
     *
     * @param messageSequence: actual sequence number of the message
     * @param expectedSequenceNumber: expected sequence to be
     * @return true if correct sequence number, false otherwise
     * @throws MessageOutOfSyncException if out of sync
     */
    public static boolean validateMessageSequenceNumber(String messageSequence, int expectedSequenceNumber) throws MessageOutOfSyncException {
        boolean isValidSequence = false;

        try {
            int actualSequenceNumber = Integer.parseInt(messageSequence);
            isValidSequence = actualSequenceNumber == expectedSequenceNumber;
        }
        catch (NumberFormatException e) {
            isValidSequence = false;
        }

        if(!isValidSequence) {
            throw new MessageOutOfSyncException();
        }

        return true;
    }

}
