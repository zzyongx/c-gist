package commons.spring;

import java.lang.reflect.*;
import java.util.Locale;
import java.util.concurrent.*;
import org.jsondoc.core.annotation.*;
import org.springframework.format.Formatter;

public class AnalogueJsonObjectFormatter<T> implements Formatter<T> {
  private Class<T> type;

  public AnalogueJsonObjectFormatter(Class<T> type) {
    this.type = type;
  }
    
  @Override
  public String print(T obj, Locale locale) {
    throw new RuntimeException("ListObjectFormatter.print is not implement");
  }
    
  @Override
  public T parse(String text, Locale locale) {
    Field[] javaFields = type.getFields();
      
    try {
      T t = type.newInstance();
      String fields[] = text.split(",");
        
      for (String field : fields) {
        String kv[] = field.split(":", 2);
        for (Field javaField : javaFields) {
          if (javaField.getName().equals(kv[0])) javaField.set(t, kv[1]);
        }
      }
      return t;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
