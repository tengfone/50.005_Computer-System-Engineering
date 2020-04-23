import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.SecureRandom;

public class ClientCP1 {

	public static void main(String[] args) {

		Boolean lastFile = false;

		String[] sendFilename = { "demo.mov","docs.pdf","Video.mp4","large.txt","buggy.txt","tenor.gif","small.txt","campus.jpg","medium.txt","guitar.wav","circus.mp3","sceneries.jpg","bytefilelarge","bytefilesmall","bytefilemedium","output_cave.wav","preprintsample.pdf","class-AudioClip.html"};
		// String[] sendFilename = {"bytefilemedium"};
		// String[] sendFilename = {"smallTestFile","mediumTestFile","largeTestFile"};
		String serverAddress = "localhost";

		int port = 4321;
		if (args.length > 2)
			port = Integer.parseInt(args[2]);

		int numBytes = 0;

		Socket clientSocket = null;

		DataOutputStream toServer = null;
		DataInputStream fromServer = null;

		FileInputStream fileInputStream = null;
		BufferedInputStream bufferedFileInputStream = null;

		try {
			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());

			// ====================AUTHENTICATION=======================

			// send a nonce
			System.out.println("Client is sending nonce");
			byte[] nonce = generateNonce();
			toServer.writeInt(nonce.length);
			toServer.write(nonce);
			toServer.flush();
			System.out.println("Nonce sent to server");

			// retrieve encrypted nonce from server
			System.out.println("Receiving encrypted nonce from server");
			byte[] encryptedNonce;
			int encryptedNonceBytes = fromServer.readInt();
			encryptedNonce = new byte[encryptedNonceBytes];
			fromServer.readFully(encryptedNonce);

			// extract public key from local directory
			System.out.println("Extracting CA public key");
			InputStream fis = new FileInputStream("./keys/cacse.crt");
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(fis);
			PublicKey caPublicKey = caCert.getPublicKey();

			// retrieve certifcate from SERVER
			System.out.println("Retreiving certificate from server");
			int certBytes = fromServer.readInt();
			byte[] certData = new byte[certBytes];
			fromServer.readFully(certData);
			InputStream certIn = new ByteArrayInputStream(certData);
			X509Certificate signedCertificate = (X509Certificate) certificateFactory.generateCertificate(certIn);

			// verify public key
			try {
				signedCertificate.checkValidity();
				signedCertificate.verify(caPublicKey);
				System.out.println("Public Key Verified");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Verification failed!");
				return;
			}

			// decrypt nonce
			System.out.println("decrypt nonce");
			PublicKey publicKeyCA = signedCertificate.getPublicKey();
			Cipher cipherDecrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipherDecrypt.init(Cipher.DECRYPT_MODE, publicKeyCA);
			byte[] decryptedNonce = cipherDecrypt.doFinal(encryptedNonce);

			// check decrypted nonce
			if (Arrays.equals(nonce, decryptedNonce)) {
				System.out.println("Authentication success!");
			} else {
				System.out.println("Authentication failed");
				return;
			}

			// ====================SEND ENCRYPTED FILE=======================
			// System.out.println("Sending file...");

			for (int j = 0; j < sendFilename.length; j++) {
				System.out.println("Sending " + sendFilename[j]);
				int lastFileIndex = sendFilename.length - 1;
				if (j == lastFileIndex) {
					lastFile = true;
				}

				String filename = sendFilename[j];
				if (args.length > 0)
					filename = args[0];
				if (args.length > 1)
					filename = args[1];

				// Start Timer
				long timeStarted = System.nanoTime();

				// Send the filename
				System.out.println("Sending Encrypted File Name");
				byte[] byteFileName = filename.getBytes();
				byte[] encryptedFileName = encryptFile(byteFileName, publicKeyCA);
				toServer.writeInt(0);
				toServer.writeInt(encryptedFileName.length);
				toServer.write(encryptedFileName);
				toServer.flush();

				// Init file input
				fileInputStream = new FileInputStream(filename);
				bufferedFileInputStream = new BufferedInputStream(fileInputStream);

				byte[] fromFileBuffer = new byte[117];

				// Send the file
				for (boolean fileEnded = false; !fileEnded;) {
					numBytes = bufferedFileInputStream.read(fromFileBuffer);
					fileEnded = numBytes < fromFileBuffer.length;

					// Print unencrypted bytes PART 1
					// System.out.println("The unencrypted bytes before encrypt: " +
					// fromFileBuffer);
					// System.out.println("The num unencrypted bytes before encrypt: " + numBytes);
					

					if (fileEnded && numBytes > 0) {
						byte[] last = new byte[numBytes];
						for (int i = 0; i < numBytes; ++i) {
							last[i] = fromFileBuffer[i];
						}

						// Print unencrypted bytes PART 2
						// System.out.println("The unencrypted bytes before decrypt: " + last);
						// System.out.println("The num unencrypted bytes before encrypt: " +
						// last.length);

						byte[] encryptedlastbit = encryptFile(last, publicKeyCA);
						int encryptedNumBytesLastBit = encryptedlastbit.length;

						// Print encrypted bytes PART 2
						// System.out.println("The encrypted bytes before decrypt: " + Base64.getEncoder().encodeToString(encryptedlastbit));
						// System.out.println("The num encrypted bytes before decrypt: " +
						// encryptedNumBytesLastBit);

						toServer.writeInt(1);
						toServer.writeInt(encryptedNumBytesLastBit);
						toServer.write(encryptedlastbit);
						toServer.flush();
						break;
					} else if (numBytes > 0) {
						byte[] encryptedFile = encryptFile(fromFileBuffer, publicKeyCA);
						int encryptedNumBytes = encryptedFile.length;

						// Print encrypted bytes PART 1
						// System.out.println("The encrypted bytes before decrypt: " + Base64.getEncoder().encodeToString(encryptedFile));
						// System.out.println("The num encrypted bytes before decrypt: " +
						// encryptedNumBytes);

						toServer.writeInt(1);
						toServer.writeInt(encryptedNumBytes);
						toServer.write(encryptedFile);
						toServer.flush();
					}
				}

				// Notify Server Finish Transferring
				if (lastFile) {
					toServer.writeInt(2);
				}
				System.out.println("Client Done");

				// Timer End
				long timeTaken = System.nanoTime() - timeStarted;
				System.out.println("Program took: " + timeTaken / 1000000.0 + "ms to run");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static byte[] generateNonce() throws NoSuchAlgorithmException {
		SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
		byte[] nonce = new byte[64];
		secureRandom.nextBytes(nonce);
		return nonce;
	}

	private static byte[] encryptFile(byte[] eachByte, PublicKey publicKeyCA) throws IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher encryptCiper = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		encryptCiper.init(Cipher.ENCRYPT_MODE, publicKeyCA);
		return encryptCiper.doFinal(eachByte);
	}

}
