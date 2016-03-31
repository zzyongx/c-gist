package commons.utils;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

public class ReflectUtil {
  @SuppressWarnings("unchecked")
  public static List<Class<? extends Enum<?>>> findEnums(String packageName) {
    try {
      ClassPathScanningCandidateComponentProvider provider =
        new ClassPathScanningCandidateComponentProvider(false);
      provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
      Set<BeanDefinition> classSet = provider.findCandidateComponents(packageName);

      List<Class<? extends Enum<?>>> enums = new ArrayList<>();
      for (BeanDefinition bean: classSet) {
        Class clazz = Class.forName(bean.getBeanClassName());
        if (clazz.isEnum()) {
          enums.add((Class<? extends Enum<?>>) clazz);
        }
      }
      return enums;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

