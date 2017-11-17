package commons.utils;

import java.util.concurrent.ThreadLocalRandom;

public class StringHelper {
  private final static String SEED = "0123456789abcdefghigklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ";
  private final static char[] HEX  = "0123456789abcdef".toCharArray();

  public static String random(int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; ++i) {
      sb.append(SEED.charAt(ThreadLocalRandom.current().nextInt(SEED.length())));
    }
    return sb.toString();
  }

  public static String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < bytes.length; ++i) {
      int v = bytes[i] & 0xFF;
      builder.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
    }
    return builder.toString();
  }
}
