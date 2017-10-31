package commons.spring.format;

import java.util.List;

public class JsonObject<T> {
  private T object;
  private List<T> list;

  public static <T> JsonObject<T> asObject(T object) {
    JsonObject<T> jsonObject = new JsonObject<T>();
    jsonObject.object = object;
    return jsonObject;
  }

  public static <T> JsonObject<T> asList(List<T> list) {
    JsonObject<T> jsonObject = new JsonObject<>();
    jsonObject.list = list;
    return jsonObject;
  }

  public T object() {
    return this.object;
  }

  public List<T> list() {
    return this.list;
  }
}
