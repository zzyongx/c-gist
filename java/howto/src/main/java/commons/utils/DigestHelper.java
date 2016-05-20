package commons.utils;

import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

public class DigestHelper {
  public static String sha1(byte[] input) {
    try {
      MessageDigest crypt = MessageDigest.getInstance("SHA-1");
      crypt.reset();
      crypt.update(input);
      return Hex.encodeHexString(crypt.digest());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String md5(byte[] input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      return Hex.encodeHexString(md.digest(input));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String hmacSHA1(String key, byte[] value) {
    try {
      byte[] keyBytes = key.getBytes();
      SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");

      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(keySpec);

      byte[] rawHmac = mac.doFinal(value);
      return Hex.encodeHexString(mac.doFinal(value));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
