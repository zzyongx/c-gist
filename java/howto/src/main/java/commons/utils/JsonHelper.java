package commons.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonHelper {
  public static <T> T readValue(String src, Class<T> valueType) {
    try {
      return new ObjectMapper().readValue(src, valueType);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static <T> T readValue(String src, TypeReference<T> valueTypeRef) {
    try {
      return new ObjectMapper().readValue(src, valueTypeRef);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static byte[] writeValueAsBytes(Object value) {
    try {
      return new ObjectMapper().writeValueAsBytes(value);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static String writeValueAsString(Object value) {
    try {
      return new ObjectMapper().writeValueAsString(value);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }
}
