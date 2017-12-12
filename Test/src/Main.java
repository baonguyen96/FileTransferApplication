import java.io.File;


public class Main {
    public static void main(String[] args) {
        testAES();
    }


    private static void testByteArray() {
        File file = new File("C:\\Users\\Bao\\Desktop\\Codes\\Java\\CS6349\\FileTransferApplication\\Assets\\Test Upload\\Notes1.txt");
        byte[] byteArray1 = new byte[(int) file.length()];
        byte[] byteArray2 = new byte[byteArray1.length + 20];

        System.out.println("byteArray1.length = " + byteArray1.length);
        System.out.println("byteArray2.length = " + byteArray2.length);
    }


    private static void testAES() {
        String key = "816 | READ2ME.txt, powerpoint presentation.pptx, Flow Diagram.pdf, Fall2017 Project Derscription Initial Draft.pdf, DSC04877.JPG, Notes2.txt, upload.txt, Notes1.txt";
        String language = AES.generateLanguage();
        AES aesEncrypt = new AES(language);
        AES aesDecrypt = new AES(language);
        String mod = "";

        System.out.println(language);
        for(int i = 1; i < 100; i++) {
            mod = aesEncrypt.encrypt(key);
            aesDecrypt.decrypt(mod);
            System.out.println();
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


    private static void testByte() {
        byte byte1 = 9;
        byte byte2 = (byte) (19 % 10);

        System.out.printf("byte1: %s\n", byte1);
        System.out.printf("byte2: %s\n", byte2);
    }

}
