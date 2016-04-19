package commons.utils;

import java.util.concurrent.ThreadLocalRandom;

public class StringHelper {
  final static String SEED = "0123456789abcdefghigklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ";
  
  public static String random(int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; ++i) {
      sb.append(SEED.charAt(ThreadLocalRandom.current().nextInt(SEED.length())));
    }
    return sb.toString();
  }
}
