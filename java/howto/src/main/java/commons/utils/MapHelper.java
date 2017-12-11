package commons.utils;

import java.util.Map;
import java.util.HashMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

public class MapHelper {
  public static Map<String, Object> make(Object ... varArgs) {
    Map<String, Object> map = new HashMap<>();

    for (int i = 0; i < varArgs.length; i+=2) {
      String key = (String) varArgs[i];
      map.put(key, varArgs[i+1]);
    }

    return map;
  }

  public static MultiValueMap<String, Object> makeMulti(Object ... varArgs) {
    MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();

    for (int i = 0; i < varArgs.length; i+=2) {
      String key = (String) varArgs[i];
      map.add(key, varArgs[i+1]);
    }

    return map;
  }

  // RestTemplate::postForObject if post map<String, String> content-type is application/x-www-form-urlencoded,
  // if post map<String, Object> content-type is multipart/form-data
  @SuppressWarnings("unchecked")
  public static <KEY, VALUE> MultiValueMap<KEY, VALUE> makeMulti2(KEY k, VALUE v, Object ... varArgs) {
    MultiValueMap<KEY, VALUE> map = new LinkedMultiValueMap<KEY, VALUE>();

    map.add(k, v);
    for (int i = 0; i < varArgs.length; i+=2) {
      map.add((KEY) varArgs[i], (VALUE) varArgs[i+1]);
    }

    return map;
  }
}
