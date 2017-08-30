package commons.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

public class AesHelper {
  private SecretKey aesKey;

  private static byte[] getBase64Byte(byte[] content) {
    if (content[content.length-1] == (byte) '\n') {
      return Arrays.copyOf(content, content.length-1);
    } else {
      return content;
    }
  }

  public AesHelper(String keyFile) {
    try {
      byte[] content = Files.readAllBytes(Paths.get(keyFile));
      byte[] decodedKey = Base64.getDecoder().decode(getBase64Byte(content));
      aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String encrypt(String input) {
    try {
      Cipher encrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
      encrypt.init(Cipher.ENCRYPT_MODE, aesKey);
      return Base64.getEncoder().encodeToString(encrypt.doFinal(input.getBytes()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String decrypt(String input) {
    try {
      Cipher encrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
      encrypt.init(Cipher.DECRYPT_MODE, aesKey);
      return new String(encrypt.doFinal(Base64.getDecoder().decode(input)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
