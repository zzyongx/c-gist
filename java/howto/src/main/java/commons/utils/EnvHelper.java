package commons.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.core.env.Environment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.EnumerablePropertySource;

public class EnvHelper {
  public static List<String> getPropertyNameImpl(
    Environment env, Predicate<String> predicate, Function<String, String> mapper) {
    
    List<String> list = new ArrayList<>();
    
    if (env instanceof ConfigurableEnvironment) {
      for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) {
        if (propertySource instanceof EnumerablePropertySource) {
          for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
            if (predicate != null) {
              if (predicate.test(key)) list.add(mapper != null ? mapper.apply(key) : key);
            } else {
              list.add(mapper != null ? mapper.apply(key) : key);
            }
          }
        }
      }
    }

    return list;
  }
    
  public static List<String> getPropertyNameWithPrefix(Environment env, String prefix) {
    return getPropertyNameImpl(env, key -> key.startsWith(prefix), null);
  }

  public static List<String> getPropertyNameSuffixWithPrefix(Environment env, String prefix) {
    return getPropertyNameImpl(env, key -> key.startsWith(prefix),
                               key -> {
                                 if (key.length() == prefix.length()) return "";
                                 else return key.substring(prefix.length()+1);
                               });
  }
}
