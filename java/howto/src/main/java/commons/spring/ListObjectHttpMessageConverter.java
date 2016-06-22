package commons.spring;

import java.io.IOException;
import java.util.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import org.springframework.util.StreamUtils;
import org.springframework.http.MediaType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;

public class ListObjectHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
  private final static Charset charset = Charset.forName("UTF-8");

  public ListObjectHttpMessageConverter() {
    super(new MediaType("application", "objectlist", charset));
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return List.class.isAssignableFrom(clazz);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
    throws IOException {
		String input = StreamUtils.copyToString(inputMessage.getBody(), charset);

    Class type =
      (Class) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
    
    Field[] javaFields = type.getFields();
    try {    
      List<Object> list = (List) clazz.newInstance();
      
      String objects[] = input.split(";");
      for (String object : objects) {
        Object t = type.newInstance();
        String fields[] = object.split(",");
        
        for (String field : fields) {
          String kv[] = field.split(":", 2);
          
          for (Field javaField : javaFields) {
            if (javaField.getName().equals(kv[0])) javaField.set(t, kv[1]);
          }
        }
        list.add(t);
      }
      return list;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
	protected void writeInternal(Object list, HttpOutputMessage outputMessage) throws IOException {
    throw new RuntimeException("ListObjectHttpMessage.writeInternal is not implemented");
	}
}
