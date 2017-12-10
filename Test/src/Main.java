public class Main {
    public static void main(String[] args) {
        testAES();
    }


    private static void testAES() {
        String key = "C:\\Users\\Bao\\Desktop\\Codes\\Java\\CS6349\\FileTransferApplication\\Assets\\Test Upload\\powerpoint presentation.pptx";
        String keyMod1 = AES.encrypt(key);
//        String keyMod2 = AES.increaseKey(key, 2);
        String keyOrg1 = AES.decrypt(keyMod1);
//        String keyOrg2 = AES.decreaseKey(keyMod2, 2);
        System.out.println(key);
        System.out.println(keyMod1);
//        System.out.println(keyMod2);
        System.out.println(keyOrg1);
//        System.out.println(keyOrg2);
    }

}
