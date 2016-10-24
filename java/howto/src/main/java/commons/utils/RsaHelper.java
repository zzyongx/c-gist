package commons.utils;

import java.nio.file.*;
import java.util.Base64;
import java.security.*;
import java.security.spec.*;
import javax.crypto.Cipher;

public class RsaHelper {
  private PrivateKey privateKey;
  private PublicKey  publicKey;

  static PrivateKey getPrivateKey(byte[] key) throws Exception {
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(keySpec);
  }

  static PublicKey getPublicKey(byte[] key) throws Exception {
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePublic(keySpec);
  }

  public RsaHelper(String privateKeyFile, String publicKeyFile) {
    try {
      privateKey = getPrivateKey(
        Base64.getDecoder().decode(
          Files.readAllBytes(Paths.get(privateKeyFile))));
      publicKey = getPublicKey(
        Base64.getDecoder().decode(
          Files.readAllBytes(Paths.get(publicKeyFile))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String encrypt(String input) {
    try {
      Cipher encrypt = Cipher.getInstance("RSA");
      encrypt.init(Cipher.ENCRYPT_MODE, publicKey);
      return Base64.getEncoder().encodeToString(encrypt.doFinal(input.getBytes()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String decrypt(String input) {
    try {
      Cipher encrypt = Cipher.getInstance("RSA");
      encrypt.init(Cipher.DECRYPT_MODE, privateKey);
      return new String(encrypt.doFinal(Base64.getDecoder().decode(input)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String sign(String input) {
    try {
      Signature signature = Signature.getInstance("SHA1withRSA");
      signature.initSign(privateKey, new SecureRandom());
      signature.update(input.getBytes());
      return new String(Base64.getEncoder().encode(signature.sign()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean verify(String input, String sign) {
    try {
      Signature signature = Signature.getInstance("SHA1withRSA");
      signature.initVerify(publicKey);
      signature.update(input.getBytes());
      return signature.verify(Base64.getDecoder().decode(sign));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
