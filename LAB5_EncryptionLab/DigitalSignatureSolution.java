import java.util.Base64;
import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.security.*;


public class DigitalSignatureSolution {

    public static void main(String[] args) throws Exception {
//Read the text file and save to String data
            String fileName = "shorttext.txt";
            String data = "",data2 = "";
            String line,line2;
            BufferedReader bufferedReader = new BufferedReader( new FileReader(fileName));
            while((line= bufferedReader.readLine())!=null){
                data = data +"\n" + line;
            }
            BufferedReader bufferedReader2 = new BufferedReader( new FileReader("longtext.txt"));
            while((line2= bufferedReader2.readLine())!=null){
                data2 = data2 +"\n" + line2;
            }

            // generate a RSA keypair, initialize as 1024 bits, get public key and private key from this keypair.
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA"); keyGen.initialize(1024); 
            KeyPair keyPair = keyGen.generateKeyPair(); 
            Key publicKey = keyPair.getPublic(); 
            Key privateKey = keyPair.getPrivate();

            // Calculate message digest, using MD5 hash function
            MessageDigest md = MessageDigest.getInstance("MD5");
            MessageDigest md2 = MessageDigest.getInstance("MD5");
            md.update(data.getBytes());
            md2.update(data2.getBytes());
            byte[] digest = md.digest();
            byte[] digest2 = md2.digest();

            // print the length of output digest byte[], compare the length of file smallSize.txt and largeSize.txt
            System.out.println("The length of output digest for shorttext.txt is: " + digest.length);
            System.out.println("The length of output digest for longtext.txt is: " + digest2.length);
           
            // Create RSA("RSA/ECB/PKCS1Padding") cipher object and initialize is as encrypt mode, use PRIVATE key.
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE,privateKey);

            // encrypt digest message
            byte[] encryptedBytes = cipher.doFinal(digest);
            byte[] encryptedBytes2 = cipher.doFinal(digest2);
            System.out.println("Signed message digest size of shorttext.txt is: " + encryptedBytes.length);
            System.out.println("Signed message digest size of longest.txt is: " + encryptedBytes2.length);

            // print the encrypted message (in base64format String using Base64)
            String base64format = Base64.getEncoder().encodeToString(encryptedBytes);
            System.out.println("base64format: " + base64format);

            // Create RSA("RSA/ECB/PKCS1Padding") cipher object and initialize is as decrypt mode, use PUBLIC key.           
            Cipher dCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            dCipher.init(Cipher.DECRYPT_MODE,publicKey);

            // decrypt message
            byte[] decryptedBytes = dCipher.doFinal(encryptedBytes);

            // print the decrypted message (in base64format String using Base64), compare with origin digest 
            String base64format2 = Base64.getEncoder().encodeToString(decryptedBytes);
            System.out.println("Decrypted Message: " + base64format2);
    }
}