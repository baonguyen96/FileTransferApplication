import javax.crypto.ShortBufferException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.copyOfRange;


public class AES implements Printable {

    // customizable combination of: {a-z} + {A-Z} + {0-9}
    private static final String LANGUAGE = "2xJIY84pWaZVt9ez0H3ncwEuGOhQP7CivAsdRDqBrlUgFjo6k1NM5XbfSLKyTm";

//    private static final String LANGUAGE = "ABCDEFGHIJ";
    private static final byte[] IV = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};


    /**
     * Encrypt message given
     *
     * @param message - Input text to be encrypted
     * @param key     - Key for encryption
     * @return cipher text in a byte array
     */
    public static byte[] encrypt(byte[] message, String key) {
        byte[] originalKey = key.getBytes();
        byte[] f_encrypted = null;
        byte[][] pt = null, encrypted = null;   //Store plain text in block of 16 bytes
        byte[] first = new byte[32];
        byte[] second = new byte[36];

        pt = padBytes(message, 20);     //Store plain text in block of 20 bytes
        encrypted = new byte[pt.length][20];

        System.arraycopy(originalKey, 0, first, 0, originalKey.length);
        System.arraycopy(IV, 0, first, originalKey.length, IV.length);
        String stringForSha1 = new String(first);
        encrypted[0] = xor(pt[0], sha1(stringForSha1));

        for (int i = 1; i < pt.length; i++) {
            System.arraycopy(originalKey, 0, second, 0, originalKey.length);
            System.arraycopy(encrypted[i - 1], 0, second, originalKey.length, encrypted[i - 1].length);
            String stringForSha12 = new String(second);
            encrypted[i] = xor(pt[i], sha1(stringForSha12));

        }
        byte[] temp = flatten(encrypted);
        f_encrypted = new byte[message.length];
        System.arraycopy(temp, 0, f_encrypted, 0, f_encrypted.length); //Truncate to original length of plain text

        if (IS_PRINTABLE) {
            System.out.println("Cipher length: " + f_encrypted.length + " bytes");
        }

        return f_encrypted;
    }


    /***
     * method: decrypt
     *
     * decrypt the message with the provided key
     *
     * @param message: encrypted message
     * @param key: key to decrypt
     *
     * @return the decrypted message in byte[]
     */
    public static byte[] decrypt(byte[] message, String key) {
        key = key.trim();
        byte[] f_decrypted = null;
        byte[][] ct, decrypted = null;
        byte[] first = new byte[32];
        byte[] second = new byte[36];
        byte[] decodedKey = new byte[16];
        decodedKey = key.getBytes();

        ct = padBytes(message, 20);     //Store cipher text in block of 20 bytes
        decrypted = new byte[ct.length][20];

        System.arraycopy(decodedKey, 0, first, 0, decodedKey.length);
        System.arraycopy(IV, 0, first, decodedKey.length, IV.length);
        String stringForSha1 = new String(first);
        decrypted[0] = xor(ct[0], sha1(stringForSha1));


        for (int i = 1; i < ct.length; i++) {
            System.arraycopy(decodedKey, 0, second, 0, decodedKey.length);
            System.arraycopy(ct[i - 1], 0, second, decodedKey.length, ct[i - 1].length);
            String stringForSha12 = new String(second);
            decrypted[i] = xor(ct[i], sha1(stringForSha12));
        }


        byte[] temp = flatten(decrypted);
        f_decrypted = new byte[message.length];
        System.arraycopy(temp, 0, f_decrypted, 0, message.length);

        if (IS_PRINTABLE) {
            System.out.println("Message length: " + f_decrypted.length + " bytes");
        }

        return f_decrypted;

    }


    /***
     * Pad cipher text in blocks
     *
     * @param source
     * @param blockSize
     *
     * @return a padded source byte[]
     */
    private static byte[][] padBytes(byte[] source, int blockSize) {
        byte[][] ret = new byte[(int) Math.ceil(source.length / (double) blockSize)][blockSize];
        int len = source.length % blockSize;
        int start = 0;
        for (int i = 0; i < ret.length; i++) {
            ret[i] = copyOfRange(source, start, start + blockSize);
            start += blockSize;
        }

        if (source.length % blockSize != 0) {
            try {
                padWithLen(ret[ret.length - 1], len, blockSize - len);
            }
            catch (ShortBufferException ex) {
                Logger.getLogger(AES.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (IS_PRINTABLE) {
            System.out.println("Number of cipher blocks: " + ret.length);
        }

        return ret;
    }


    /***
     * Flatten 2D arrays in a 1D array
     *
     * @param arr - the 2D input array
     *
     * @return 1D representation of arr
     */
    private static byte[] flatten(byte[][] arr) {
        List<Byte> list = new ArrayList<>();
        for (byte[] arr1 : arr) {
            for (byte anArr1 : arr1) {
                list.add(anArr1);
            }
        }

        byte[] vector = new byte[list.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = list.get(i);
        }
        return vector;
    }


    /**
     * Xor function for two arrays of bytes
     *
     * @param array1: first array to XOR
     * @param array2: second array to XOR
     * @return XOR of the 2 arrays
     */
    private static byte[] xor(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length];

        for(int i = 0; i < array1.length; i++) {
            result[i] = (byte)(array1[i] ^ array2[i]);
        }

        return result;
    }


    /**
     * Adds the given number of padding bytes to the data input. The value of
     * the padding bytes is determined by the specific padding mechanism that
     * implements this interface.
     *
     * @param in  the input buffer with the data to pad
     * @param off the offset in <code>in</code> where the padding bytes are
     *            appended
     * @param len the number of padding bytes to add
     * @throws ShortBufferException if <code>in</code> is too small to hold
     *                              the padding bytes
     */
    private static void padWithLen(byte[] in, int off, int len)
            throws ShortBufferException {
        if (in == null) {
            return;
        }

        if ((off + len) > in.length) {
            throw new ShortBufferException("Buffer too small to hold padding");
        }

        byte paddingOctet = (byte) (len & 0xff);
        for (int i = 0; i < len; i++) {
            in[i + off] = paddingOctet;
        }
    }


    /***
     * Use sha1 to process message
     *
     * @param message: the message to be processed
     * @return the resulting message
     */
    private static byte[] sha1(String message) {
        byte[] sha1Encode = null;

        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA");
            sha1Encode = sha1Digest.digest(message.getBytes());
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return sha1Encode;

    }


    /***
     * method: getRandomString
     *
     * build a random string with custom length from the character set
     *
     * @param length: length of the random string to build
     * @return a random string
     */
	public static String getRandomString(int length) {
	    StringBuilder sb = new StringBuilder();
	    int len = LANGUAGE.length();
	    for (int i = 0; i < length; i++) {
	       sb.append(LANGUAGE.charAt((int) Math.round(Math.random() * (len - 1))));
	    }
	    return sb.toString();
	}


    /***
     * method: increaseKey
     *
     * implement a poly-alphabetic cipher to reduce the possibility of being hacked
     * add offset to the original key to get the new key
     * value of new key depends on:
     *      the location of each character in the original key +
     *      the value of the character in the LANGUAGE +
     *      the offset
     *
     * @param original: original key
     * @param offset: how far off the original key is the new key
     * @return the new key
     */
    public static String increaseKey(String original, int offset) {
        StringBuilder modified = new StringBuilder();
        char c = 0;
        int difference = 0, newPosition = 0, buffer = LANGUAGE.length() / 5;

        for(int i = 0; i < original.length(); i++) {
            c = original.charAt(i);
            difference = i + (offset + 1) * buffer;
            newPosition = (LANGUAGE.indexOf(c) + difference) % LANGUAGE.length();
            c = LANGUAGE.charAt(newPosition);
            modified.append(c);
        }

        return modified.toString();
    }


    /***
     * method: decreaseKey
     *
     * implement a poly-alphabetic cipher to reduce the possibility of being hacked
     * add offset to the original key to get the new key
     * value of new key depends on:
     *      the location of each character in the original key +
     *      the value of the character in the LANGUAGE +
     *      the offset
     *
     * @param modified: modified key
     * @param offset: how far off the original key is the new key
     * @return the original key
     */
    public static String decreaseKey(String modified, int offset) {
        StringBuilder original = new StringBuilder();
        char c = 0;
        int difference = 0, newPosition = 0, buffer = LANGUAGE.length() / 5, temp = 0;

        for(int i = 0; i < modified.length(); i++) {
            c = modified.charAt(i);
            difference = i + (offset + 1) * buffer;
            temp = LANGUAGE.indexOf(c) - difference;

            if(temp >= 0) {
                newPosition = temp;
            }
            else {
                difference -= LANGUAGE.indexOf(c);
                newPosition = LANGUAGE.length() - difference;
            }

            c = LANGUAGE.charAt(newPosition);
            original.append(c);
        }

        return original.toString();
    }
}
