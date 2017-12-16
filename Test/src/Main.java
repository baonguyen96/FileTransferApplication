public class Main {
    public static void main(String[] args) {
        testAES();
    }


    private static void testAES() {
        String key = "816 | READ2ME.txt, powerpoint presentation.pptx, Flow Diagram.pdf, Fall2017 Project Derscription Initial Draft.pdf, DSC04877.JPG, Notes2.txt, upload.txt, Notes1.txt";
        String language = AES.generateLanguage();
        AES aesEncrypt = new AES(language);
        AES aesDecrypt = new AES(language);
        String mod = "";

        System.out.println(language);
        for (int i = 1; i < 100; i++) {
            mod = aesEncrypt.encrypt(key);
            System.out.printf("Repeat: %d\n\n", testRepeat(key, mod));
//            aesDecrypt.decrypt(mod);
//            System.out.println();
        }


//        System.out.println(key);
//        System.out.println(language);
//
//        String keyMod1 = aes.increaseKey(key, 6);
//        System.out.println(keyMod1);

//        String keyMod2 = aes.encrypt(key);
//        System.out.println(keyMod2);
//
//        String keyOrg1 = aes.decreaseKey(keyMod1, 6);
//        System.out.println(keyOrg1);

//        String keyOrg2 = aes.decrypt(keyMod2);
//        System.out.println(keyOrg2);
    }


    private static int testRepeat(String plain, String cipher) {
        for(int i = 0; i < plain.length(); i++) {
            if(plain.charAt(i) == cipher.charAt(i)) {
                return i;
            }
        }
        return -1;
    }


    private static void testByte() {
        byte byte1 = 9;
        byte byte2 = (byte) (19 % 10);

        System.out.printf("byte1: %s\n", byte1);
        System.out.printf("byte2: %s\n", byte2);
    }

}
