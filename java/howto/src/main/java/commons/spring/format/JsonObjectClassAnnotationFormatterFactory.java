package commons.spring.format;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;


public class JsonObjectClassAnnotationFormatterFactory implements AnnotationFormatterFactory<JsonObjectClass> {
  private static final Set<Class<?>> FIELD_TYPES;
  static {
    FIELD_TYPES = new HashSet<Class<?>>();
    FIELD_TYPES.add(JsonObject.class);
  }

  @Override
  public Set<Class<?>> getFieldTypes() {
    return FIELD_TYPES;
  }

  @Override
  public Printer<?> getPrinter(JsonObjectClass annotation, Class<?> fieldType) {
    return getFormatter(annotation, fieldType);
  }

  @Override
  public Parser<?> getParser(JsonObjectClass annotation, Class<?> fieldType) {
    return getFormatter(annotation, fieldType);
  }

  protected Formatter<JsonObject> getFormatter(JsonObjectClass annotation, Class<?> fieldType) {
    return new JsonObjectFormatter(annotation.value(), annotation.islist());
  }
}
