import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ServerCP2 {
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
			// toClient.flush();

			// Send Sign Certificate
			System.out.println("Sending signed certificate");
			Path path = Paths.get("./keys/CA.crt");
			byte[] data = Files.readAllBytes(path);
			toClient.writeInt(data.length);
			toClient.write(data);

			// ====================End of Authentication=======================

			// ====================Decrypt Session Key=======================
			System.out.println("Decrypting Session Key");

			// receiving & decrypt the Session Key
			int AESnumBytes = fromClient.readInt();
			byte[] AESSessionKey = new byte[AESnumBytes];
			fromClient.readFully(AESSessionKey, 0, AESnumBytes);
			byte[] aesSessionKey = decryptSession(AESSessionKey, serverPrivateKey);
			SecretKey sessionKey = new SecretKeySpec(aesSessionKey, 0, aesSessionKey.length, "AES");
			System.out.println("Session Key Decrypted");

			// BufferedReader messageIn = new BufferedReader(new
			// InputStreamReader(connectionSocket.getInputStream()));

			while (!connectionSocket.isClosed()) {
				try {
					// receiving and decrypt file name
					int packetType = fromClient.readInt();
					if (packetType == 0) {
						int encryptedFileNamenumBytes = fromClient.readInt();
						byte[] encryptedFileName = new byte[encryptedFileNamenumBytes];
						fromClient.readFully(encryptedFileName, 0, encryptedFileNamenumBytes);
						byte[] decryptedFileName = decryptFile(encryptedFileName, sessionKey);

						fileOutputStream = new FileOutputStream(
								"recv_CP2_" + new String(decryptedFileName, 0, decryptedFileName.length));
						bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);
						// Packet Data
					} else if (packetType == 1) {
						// receieving & decrypted file
						int encryptednumBytes = fromClient.readInt();
						byte[] encryptedFile = new byte[encryptednumBytes];
						fromClient.readFully(encryptedFile, 0, encryptednumBytes);

						// Print received encrypted bytes
						// System.out.println("The received encrypted bytes are: " + Base64.getEncoder().encodeToString(block));
						// System.out.println("The receieved int encrypted bytes are: " + numBytes);

						byte[] decryptedFile = decryptFile(encryptedFile, sessionKey);

						// Print received decrypted bytes
						// System.out.println("The received decrypted bytes are: " + deCryptedChunk);
						// System.out.println("The receieved int decrypted bytes are: " +
						// deCryptedChunk.length);

						// create new file and write to file
						if (encryptednumBytes > 0) {
							bufferedFileOutputStream.write(decryptedFile, 0, decryptedFile.length);
							bufferedFileOutputStream.flush();
						}
						if (encryptednumBytes < 117) {
							System.out.println("Closing connection...");
							if (bufferedFileOutputStream != null)
								bufferedFileOutputStream.close();
							if (bufferedFileOutputStream != null)
								fileOutputStream.close();
							bufferedFileOutputStream.flush();
						}
						// Connection close
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

	private static byte[] decryptFile(byte[] eachByte, SecretKey sessionKey) throws IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher decryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		decryptCipher.init(Cipher.DECRYPT_MODE, sessionKey);
		return decryptCipher.doFinal(eachByte);
	}

	private static byte[] decryptSession(byte[] eachByte, PrivateKey privateKey) throws IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
		return decryptCipher.doFinal(eachByte);
	}
}
