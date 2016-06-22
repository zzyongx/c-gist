package commons.saas;

import java.util.Map;
import java.util.HashMap;
import org.springframework.core.env.Environment;

public class RestNameService {
  Map<String, String> map = new HashMap<>();
  
  public RestNameService(Environment env) {
    String token = null;
    
    for (int i = 1; i < 101; ++i) {
      String value = env.getProperty("rest.nameservice." + String.valueOf(i));
      if (value == null) continue;

      if (token == null) token = env.getRequiredProperty("rest.token");

      String parts[] = value.split(":", 2);
      map.put(parts[0], parts[1].replace("__token__", token));
    }
  }

  public String lookup(String name) {
    String value = map.get(name);
    if (value == null) {
      throw new RuntimeException(name + " is not found in " + map.toString());
    }
    return value;
  }
}
