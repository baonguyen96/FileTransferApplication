/*
 * demo of string encryption/decryption
 */

import java.nio.charset.StandardCharsets;


public class Main {
    public static void main(String[] args) {
        String s = "1 | this is something else | hello";
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);      // convert UTF8 String to byte[]
        String n = new String(bytes, StandardCharsets.UTF_8);   // use this to convert byte[] to String
        byte[] e = AES.encrypt(bytes, "aaaaaaaaaaaaaaaa");
        byte[] d = AES.decrypt(e, "aaaaaaaaaaaaaaaa");
        String decr = new String(d, StandardCharsets.UTF_8);

        System.out.println("s     : " + s);
        print(bytes, "bytes");
        System.out.println("n     : " + n);
        print(e, "e");
        print(d, "d");
        System.out.println("decr  : " + decr);
    }

    static byte[] encrypt(byte[] bytes) {
        byte[] cipher = new byte[bytes.length];

        for(int i = 0; i < cipher.length; i++) {
            cipher[i] = (byte) (bytes[i] + 1);
        }

        return cipher;
    }

    static void print(byte[] bytes, String name) {
        System.out.printf("%-6s:", name);
        for(byte b : bytes) {
            System.out.printf(" %s(%c)", b, (char)b);
        }
        System.out.println();
    }
}
