import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class Main {
    public static void main(String[] args) {
        String org = AES.getRandomString(16);
        System.out.println(org);
        System.out.println(AES.modifyKey(org, 1));
        System.out.println(AES.modifyKey(org, 2));
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
}
