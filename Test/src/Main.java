public class Main {
    public static void main(String[] args) {
        testByte();
    }


    private static void testAES() {
        String key = "C:\\Users\\Bao\\Desktop\\Codes\\Java\\CS6349\\FileTransferApplication\\Assets\\Supplementals\\Authentication Protocol.txt";
        String language = "gX.59z\\CbSFReQn:OZ\"GKlMoqPTBxVp1y4DH3N,|vsa78YEUmA6wJdti rLjhIW2cu0fk";
        AES aes = new AES(language);

        System.out.println(key);
        System.out.println(language);

        String keyMod1 = aes.encrypt(key);
        System.out.println(keyMod1);

        String keyMod2 = aes.increaseKey(key, 2);
        System.out.println(keyMod2);

        String keyOrg1 = aes.decrypt(keyMod1);
        System.out.println(keyOrg1);

        String keyOrg2 = aes.decreaseKey(keyMod2, 2);
        System.out.println(keyOrg2);
    }


    private static void testByte() {
        byte byte1 = 9;
        byte byte2 = (byte) (19 % 10);

        System.out.printf("byte1: %s\n", byte1);
        System.out.printf("byte2: %s\n", byte2);
    }

}
