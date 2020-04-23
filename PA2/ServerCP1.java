import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ServerCP1 {

	public static void main(String[] args) {
		int port = 4321;
		if (args.length > 0)
			port = Integer.parseInt(args[0]);

		ServerSocket welcomeSocket = null;
		Socket connectionSocket = null;
		DataOutputStream toClient = null;
		DataInputStream fromClient = null;

		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedFileOutputStream = null;

		try {
			welcomeSocket = new ServerSocket(port);
			connectionSocket = welcomeSocket.accept();
			fromClient = new DataInputStream(connectionSocket.getInputStream());
			toClient = new DataOutputStream(connectionSocket.getOutputStream());

			// ====================Authentication=======================
			int nonceSize = fromClient.readInt();
			byte[] clientNonce = new byte[nonceSize];
			fromClient.readFully(clientNonce);

			// create cipher object, initalize cipher with server public key, RSA encryption
			// mode
			Cipher desCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			PrivateKey serverPrivateKey = getPrivateKey("./keys/private_key.der");
			desCipher.init(Cipher.ENCRYPT_MODE, serverPrivateKey);

			// Encrypt Nonce from Server and sent to client
			System.out.println("Encrypt Nonce from Server and sent to client");
			byte[] encryptedNonce = desCipher.doFinal(clientNonce);
			toClient.writeInt(encryptedNonce.length);
			toClient.write(encryptedNonce);

			// Send Sign Certificate
			System.out.println("Sending signed certificate");
			Path path = Paths.get("./keys/CA.crt");
			byte[] data = Files.readAllBytes(path);
			toClient.writeInt(data.length);
			toClient.write(data);

			// ====================End of Authentication=======================

			// ====================Decrypt File=======================
			System.out.println("Decrypting FILE");
			Cipher cipherDecrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipherDecrypt.init(Cipher.DECRYPT_MODE, serverPrivateKey);

			// receieve file from stream
			System.out.println("Receieve file from stream");

			while (!connectionSocket.isClosed()) {
				try {
					int packetType = fromClient.readInt();

					// If the packet is for transferring the filename
					if (packetType == 0) {

						System.out.println("Receiving filename...");

						int numBytes = fromClient.readInt();
						byte[] FILENAME = new byte[numBytes];
						// Must use read fully!
						// See:
						// https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
						fromClient.readFully(FILENAME, 0, numBytes);

						byte[] decryptedFileName = decryptFile(FILENAME, serverPrivateKey);

						fileOutputStream = new FileOutputStream(
								"recv_" + new String(decryptedFileName, 0, decryptedFileName.length));
						bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);
						System.out.println("Filename retrieved");

						// If the packet is for transferring a chunk of the file
					} else if (packetType == 1) {
						// read numBytes LENGTH of ENCRYPTED chunk
						int numBytes = fromClient.readInt(); // 128
						byte[] block = new byte[numBytes];
						fromClient.readFully(block, 0, numBytes);

						// Print received encrypted bytes
						// System.out.println("The received encrypted bytes are: " + Base64.getEncoder().encodeToString(block));
						// System.out.println("The receieved int encrypted bytes are: " + numBytes);

						// Decrypt the new encrypted chunk
						byte[] deCryptedChunk = decryptFile(block, serverPrivateKey);

						// Print received decrypted bytes
						// System.out.println("The received decrypted bytes are: " + deCryptedChunk);
						// System.out.println("The receieved int decrypted bytes are: " +
						// deCryptedChunk.length);

						if (numBytes > 0) {
							bufferedFileOutputStream.write(deCryptedChunk, 0, deCryptedChunk.length);
							bufferedFileOutputStream.flush();
						}
						if (numBytes < 117) {
							System.out.println("Closing Current File Stream...");

							if (bufferedFileOutputStream != null)
								bufferedFileOutputStream.close();
							if (bufferedFileOutputStream != null)
								fileOutputStream.close();
							bufferedFileOutputStream.flush();
						}
						// Close connection
					} else if (packetType == 2) {
						System.out.println("Client Is Done With Transferring");
						fromClient.close();
						toClient.close();
						connectionSocket.close();
					}
				} catch (EOFException e) {
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Read private key
	public static PrivateKey getPrivateKey(String filename) throws Exception {
		byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	private static byte[] decryptFile(byte[] eachByte, PrivateKey privateKey) throws IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
		return decryptCipher.doFinal(eachByte);
	}
}