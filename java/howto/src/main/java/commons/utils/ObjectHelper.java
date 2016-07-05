package commons.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ObjectHelper {
  static String dumpImpl(Object obj) throws Exception {
    if (obj == null) return "NIL";
                       
    Class clazz = obj.getClass();
    String pkg = clazz.getPackage().getName();
    
    StringBuilder builder = new StringBuilder();

    String separator = "";
    if (Collection.class.isAssignableFrom(clazz)) {
      Collection collection = (Collection) obj;
      builder.append("[");
      for (Object o : collection) {
        builder.append(separator).append(dumpImpl(o));
        separator = ", ";
      }
      builder.append("]");
      return builder.toString();
    } else if (Map.class.isAssignableFrom(clazz)) {
      Map map = (Map) obj;
      
      @SuppressWarnings("unchecked")
      Set<Map.Entry> set = (Set<Map.Entry>) map.entrySet();
      
      builder.append("{");
      for (Map.Entry entry : set) {
        builder.append(separator).append(dumpImpl(entry.getKey()))
          .append("=").append(dumpImpl(entry.getValue()));
        separator = ", ";
      }
      builder.append("}");
      return builder.toString();
    } else if (String.class.isAssignableFrom(clazz)) {
      return (String) obj;
    } else if(pkg.startsWith("java.time") || clazz.isEnum()) {
      return obj.toString();
    } else {
      return dumpPojo(obj);
    }
  }

  static String dumpPojo(Object obj) throws Exception {
    Class clazz = obj.getClass();
    StringBuilder builder = new StringBuilder();

    String separator = "";
    Field[] fields = clazz.getFields();
    for (Field field : fields) {
      int modifier = field.getModifiers();
      if (Modifier.isFinal(modifier)) continue;
      
      builder.append(separator).append(field.getName()).append("=")
        .append(dumpImpl(field.get(obj)));
      separator = "; ";
    }

    Method[] methods = clazz.getMethods();
    for (Method method : methods) {
      String methodName = method.getName();
      if (methodName.equals("getClass")) continue;
      if (methodName.length() < 4) continue;
      
      if (methodName.startsWith("get") && method.getParameterCount() == 0) {
        String name;
        if (methodName.length() == 4) {
          name = methodName.substring(3).toLowerCase();
        } else {
          name = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        }

        builder.append(separator).append(name).append("=")
          .append(dumpImpl(method.invoke(obj)));
        separator = "; ";
      }
    }

    String s = builder.toString();
    return s.isEmpty() ? obj.toString() : "{" + s + "}";
  }

  public static String dump(Object obj) {
    try {
      return dumpImpl(obj);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
