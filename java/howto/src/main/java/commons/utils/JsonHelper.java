package commons.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonHelper {
  static ObjectMapper mapper = new ObjectMapper();  
  static SimpleModule module = new SimpleModule("java.time.*");
  static {
    module.addSerializer(LocalDate.class, new LocalDateJsonSerializer());
    module.addDeserializer(LocalDate.class, new LocalDateJsonDeserializer());
    module.addSerializer(LocalDateTime.class, new LocalDateTimeJsonSerializer());
    module.addDeserializer(LocalDateTime.class, new LocalDateTimeJsonDeserializer());
    
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
    mapper.registerModule(module);
  }

  public static ObjectMapper factory() {
    return mapper;
  }

  public static <T> T readValue(String src, Class<T> valueType) {
    try {
      return factory().readValue(src, valueType);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static <T> T readValue(String src, TypeReference<T> valueTypeRef) {
    try {
      return factory().readValue(src, valueTypeRef);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static byte[] writeValueAsBytes(Object value) {
    try {
      return factory().writeValueAsBytes(value);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static String writeValueAsString(Object value) {
    try {
      return factory().writeValueAsString(value);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }
}
