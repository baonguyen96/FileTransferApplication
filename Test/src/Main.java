public class Main {
    public static void main(String[] args) {
        testAES();
    }


    private static void testAES() {
        String key = AES.getRandomString(16);
        String keyMod1 = AES.increaseKey(key, 1);
        String keyMod2 = AES.increaseKey(key, 2);
        String keyOrg1 = AES.decreaseKey(keyMod1, 1);
        String keyOrg2 = AES.decreaseKey(keyMod2, 2);
        System.out.println(key);
        System.out.println(keyMod1);
        System.out.println(keyMod2);
        System.out.println(keyOrg1);
        System.out.println(keyOrg2);
    }

}
