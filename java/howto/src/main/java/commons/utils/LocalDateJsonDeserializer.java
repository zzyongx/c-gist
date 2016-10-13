package commons.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class LocalDateJsonDeserializer extends JsonDeserializer<LocalDate> {
  static DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    String string = parser.getText().trim();
    return LocalDate.parse(string, dtFmt);
  }
}
