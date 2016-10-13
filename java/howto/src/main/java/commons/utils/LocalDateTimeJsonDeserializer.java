package commons.utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class LocalDateTimeJsonDeserializer extends JsonDeserializer<LocalDateTime> {
  static DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    String string = parser.getText().trim();
    return LocalDateTime.parse(string, dtFmt);
  }
}
