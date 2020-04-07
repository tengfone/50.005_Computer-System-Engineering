import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import javax.crypto.*;

public class DesImageSolution {
    public static void main(String[] args) throws Exception {
        int image_width = 200;
        int image_length = 200;
        // read image file and save pixel value into int[][] imageArray
        BufferedImage img = ImageIO.read(new File("triangle.bmp"));
        image_width = img.getWidth();
        image_length = img.getHeight();
        // byte[][] imageArray = new byte[image_width][image_length];
        int[][] imageArray = new int[image_width][image_length];
        for (int idx = 0; idx < image_width; idx++) {
            for (int idy = 0; idy < image_length; idy++) {
                int color = img.getRGB(idx, idy);
                imageArray[idx][idy] = color;
            }
        }

        // x is width (how width) off set from horizontal, y is top to length(how tall)
        // vertical.
        // a column is fix x vary y
        // R G B ALPHA
        // SAME SIZE AS JAVA INTEGER

        // generate secret key using DES algorithm
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey desKey = keyGen.generateKey();

        // Create cipher object, initialize the ciphers with the given key, choose
        // encryption algorithm/mode/padding,
        // you need to try both ECB and CBC mode, use PKCS5Padding padding method
        Cipher desCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        desCipher.init(Cipher.ENCRYPT_MODE, desKey);

        // define output BufferedImage, set size and format
        BufferedImage outImage = new BufferedImage(image_width, image_length, BufferedImage.TYPE_3BYTE_BGR);

        for (int idx = 0; idx < image_width; idx++) {
            // convert each column int[] into a byte[] (each_width_pixel)
            byte[] each_width_pixel = new byte[4 * image_length];
            for (int idy = 0; idy < image_length; idy++) {
                ByteBuffer dbuf = ByteBuffer.allocate(4);
                dbuf.putInt(imageArray[idx][idy]);
                byte[] bytes = dbuf.array();
                // From Top To Bottom
                System.arraycopy(bytes, 0, each_width_pixel, idy * 4, 4);
            }

            // encrypt each column or row bytes
            byte[] encryptedBytesArray = desCipher.doFinal(each_width_pixel);
            byte[] encrypted_pixel = new byte[4];
            for (int idy = 0; idy < image_length; idy++) {
                // From Top To Bottom
                System.arraycopy(encryptedBytesArray, idy * 4, encrypted_pixel, 0, 4);
                ByteBuffer wrapped = ByteBuffer.wrap(encrypted_pixel);
                // convert the encrypted byte[] back into int[] and write to outImage (use
                // setRGB)
                int newcolor = wrapped.getInt();
                outImage.setRGB(idx, idy, newcolor);
            }

            // write outImage into file
            ImageIO.write(outImage, "BMP", new File("EnSUTD.bmp"));
        }
    }

    public static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

}