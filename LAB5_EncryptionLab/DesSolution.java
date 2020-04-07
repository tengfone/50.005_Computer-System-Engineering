import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.crypto.*;
import java.util.Base64;


public class DesSolution {
    public static void main(String[] args) throws Exception {
        String fileName = "shorttext.txt";
        String data = "";
        String data2 = "";
        String line;
        BufferedReader bufferedReader = new BufferedReader( new FileReader(fileName));
        while((line= bufferedReader.readLine())!=null){
            data = data +"\n" + line;
        }
        System.out.println("Original content: "+ data + "\n");
           
        // generate secret key using DES algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey desKey = keyGen.generateKey();

        // create cipher object, initialize the ciphers with the given key, choose encryption mode as DES
        Cipher desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, desKey);

        // do encryption, by calling method Cipher.doFinal().
        byte[] encryptedBytesArray = desCipher.doFinal(data.getBytes());


        // do format conversion. Turn the encrypted byte[] format into base64format String using Base64
        String base64format = Base64.getEncoder().encodeToString(encryptedBytesArray);

        // print the encrypted message (in base64format String format)
        System.out.println("base64format: " + base64format + "\n");

        // create cipher object, initialize the ciphers with the given key, choose decryption mode as DES
        Cipher decrypt = Cipher.getInstance("DES");

        decrypt.init(Cipher.DECRYPT_MODE,desKey);

        // do decryption, by calling method Cipher.doFinal().
        byte[] byte_array = decrypt.doFinal(encryptedBytesArray);

        // do format conversion. Convert the decrypted byte[] to String, using "String a = new String(byte_array);"
        String a = new String(byte_array);

        // print the decrypted String text and compare it with original text
        System.out.println("Original Text: " + data + "\n\n\n" + "Decrypted String: " + a + "\n");

        // print the length of output encrypted byte[], compare the length of file smallSize.txt and largeSize.txt
        BufferedReader bufferedReader2 = new BufferedReader( new FileReader("longtext.txt"));
        while((line= bufferedReader2.readLine())!=null){
            data2 = data2 +"\n" + line;
        }
        byte[] encryptedBytesArray2 = desCipher.doFinal(data2.getBytes());

        int shorttext = encryptedBytesArray.length;
        int longtext = encryptedBytesArray2.length;
        System.out.println("The length of shorttext.txt in terms of byte[] format is: " + shorttext + "\nThe length of longtext.txt in terms of byte[] format is: " + longtext);
    }
}


