package commons.spring.format;

import java.util.List;
import java.util.Locale;
import org.springframework.format.Formatter;
import commons.utils.JsonHelper;

public class JsonObjectFormatter implements Formatter<JsonObject> {
  private Class<?> type;
  private Boolean listType;

  public JsonObjectFormatter() {}

  public JsonObjectFormatter(Class<?> type, boolean listType) {
    this.type = type;
    this.listType = listType;
  }

  public void setType(Class<?> type) {
    this.type = type;
  }

  public void setListType(boolean listType) {
    this.listType = listType;
  }

  @Override
  public String print(JsonObject obj, Locale locale) {
    throw new RuntimeException("JsonObjectFormatter.print is not implement");
  }

  @Override
  public JsonObject<?> parse(String text, Locale locale) {
    if (type == null) throw new RuntimeException("JsonObject<T> need @JsonObjectClass(Foo.class, islist=true)");

    if (listType) {
      List<?> list = JsonHelper.readListValue(text, type);
      return JsonObject.asList(list);
    } else {
      Object object = JsonHelper.readValue(text, type);
      return JsonObject.asObject(object);
    }
  }
}
