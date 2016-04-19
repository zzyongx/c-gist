package commons.utils;

import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;

public class DigestHelper {
  public static String sha1(byte[] input) {
    try {
      MessageDigest crypt = MessageDigest.getInstance("SHA-1");
      crypt.reset();
      crypt.update(input);
      return Hex.encodeHexString(crypt.digest());
    } catch (Exception e) {
      throw new RuntimeException("SHA-1 ENV config error");
    }
  }

  public static String md5(byte[] input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      return Hex.encodeHexString(md.digest(input));
    } catch (Exception exception) {
      throw new RuntimeException("MD5 config error");
    }
  }
}
