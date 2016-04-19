package commons.spring;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import org.jsondoc.core.annotation.*;
import org.springframework.format.Formatter;

public class ListObjectFormatter<T> implements Formatter<List<T>> {
  private Class<T> type;
  public ListObjectFormatter(Class<T> type) {
    this.type = type;
  }
    
  @Override
  public String print(List<T> list, Locale locale) {
    throw new RuntimeException("ListObjectFormatter.print is not implement");
  }
    
  @Override
  public List<T> parse(String text, Locale locale) {
    Field[] javaFields = type.getFields();
      
    String objects[] = text.split(";");
    List<T> list = new ArrayList<T>();

    for (String object : objects) {
      try {
        T t = type.newInstance();
        String fields[] = object.split(",");
        
        for (String field : fields) {
          String kv[] = field.split(":", 2);
          for (Field javaField : javaFields) {
            if (javaField.getName().equals(kv[0])) javaField.set(t, kv[1]);
          }
        }
        list.add(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return list;
  }
}
