public class Main {
    public static void main(String[] args) {
        testAES();
    }


    private static void testAES() {
        String key = "514 | stay";
        String language = AES.generateLanguage();
        AES aes = new AES(language);

        System.out.println(key);
        System.out.println(language);

        String keyMod1 = aes.encrypt(key);
        System.out.println(keyMod1);

        String keyMod2 = aes.encrypt(key);
        System.out.println(keyMod2);

        String keyOrg1 = aes.decrypt(keyMod1);
        System.out.println(keyOrg1);

        String keyOrg2 = aes.decrypt(keyMod2);
        System.out.println(keyOrg2);
    }


    private static void testByte() {
        byte byte1 = 9;
        byte byte2 = (byte) (19 % 10);

        System.out.printf("byte1: %s\n", byte1);
        System.out.printf("byte2: %s\n", byte2);
    }

}
