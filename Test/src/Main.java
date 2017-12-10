public class Main {
    public static void main(String[] args) {
        try {
            testAES();
        }
        catch (Exception e) {
            System.out.println("Opps");
            e.printStackTrace();
        }
    }


    private static void testAES() {
        String key = "C:\\Users\\Bao\\Desktop\\Codes\\Java\\CS6349\\FileTransferApplication\\Assets\\Supplementals\\Authentication Protocol.txt";
        String language = "gX.59z\\CbSFReQn:OZ\"GKlMoqPTBxVp1y4DH3N,|vsa78YEUmA6wJdti rLjhIW2cu0fk";
        AES.setLanguage(language);

        System.out.println(key);
        System.out.println(language);

        String keyMod1 = AES.encrypt(key);
        System.out.println(keyMod1);

        String keyMod2 = AES.increaseKey(key, 2);
        System.out.println(keyMod2);

        String keyOrg1 = AES.decrypt(keyMod1);
        System.out.println(keyOrg1);

        String keyOrg2 = AES.decreaseKey(keyMod2, 2);
        System.out.println(keyOrg2);
    }

}
