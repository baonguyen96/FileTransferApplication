import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class Main {
    public static void main(String[] args) {
        // can try with 44darxoD8UZqXtZ4 to see the poly-alphabetic cipher
        String key = AES.getRandomString(16);
        System.out.println(key);
        System.out.println(AES.increaseKey(key, 1));
        System.out.println(AES.increaseKey(key, 2));
        System.out.println(AES.increaseKey(key, 1));
        System.out.println(AES.increaseKey(key, 2));
        System.out.printf("Length: %d byte(s)\n", key.length());
    }


    private static void shuffleArray(char[] ar) {
        // If running on Java 6 or older, use `new Random()` on RHS here
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            char a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }

        for(char c : ar) {
            System.out.print(c);
        }
        System.out.println();
    }

    private static void printCharSet() {
        for(char c = 'a'; c <= 'z'; c++) {
            System.out.print(c);
        }
        for(char c = '0'; c <= '9'; c++) {
            System.out.print(c);
        }
        for(char c = 'A'; c <= 'Z'; c++) {
            System.out.print(c);
        }
    }
}
