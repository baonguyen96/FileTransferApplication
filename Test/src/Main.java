import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class Main {
    public static void main(String[] args) {
        testString();
    }


    public static void testString() {
        String s = "1 | this is something else | hello";
        byte[] sBytes = s.getBytes(StandardCharsets.ISO_8859_1);
        String newStr = new String(sBytes, StandardCharsets.ISO_8859_1);
        byte[] e = AES.encrypt(newStr.getBytes(StandardCharsets.ISO_8859_1), "aaaaaaaaaaaaaaaa");
        String encr = new String(e, StandardCharsets.ISO_8859_1);
        byte[] d = AES.decrypt(encr.getBytes(StandardCharsets.ISO_8859_1), "aaaaaaaaaaaaaaaa");
        String decr = new String(d, StandardCharsets.ISO_8859_1);

        System.out.println("s     : " + s);
        System.out.println();

        print(sBytes, "sBytes");
        System.out.println();

        System.out.println("newStr : " + newStr);
        System.out.println();

        print(e, "e");
        System.out.println();

        System.out.println("encr  :" + encr);
        System.out.println();

        print(d, "d");
        System.out.println();

        System.out.println("decr  : " + decr);
        System.out.println();
    }


    static void print(byte[] bytes, String name) {
        System.out.printf("%-6s:", name);
        for(byte b : bytes) {
            System.out.printf(" %s(%c)", b, (char)b);
        }
        System.out.println();
    }


    public static void testAES() {
        String key = AES.getRandomString(16);
        System.out.println(key);
        System.out.println(AES.modifyKey(key, 1));
        System.out.println(AES.modifyKey(key, 2));
        System.out.println(AES.modifyKey(key, 1));
        System.out.println(AES.modifyKey(key, 2));
        System.out.printf("Length: %d byte(s)\n", key.length());
    }


    private static String generateCharSet() {
        final String LANGUAGE = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        char[] ar = LANGUAGE.toCharArray();

        // If running on Java 6 or older, use `new Random()` on RHS here
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            char a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }

        return new String(ar);

    }

}
