package commons.utils;

import java.util.Map;
import java.util.HashMap;

public class MapHelper {
  public static Map<String, Object> make(Object ... varArgs) {
    Map<String, Object> map = new HashMap<>();

    for (int i = 0; i < varArgs.length; i+=2) {
      String key = (String) varArgs[i];
      map.put(key, varArgs[i+1]);
    }

    return map;
  }
}
