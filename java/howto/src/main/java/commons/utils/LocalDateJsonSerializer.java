package commons.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class LocalDateJsonSerializer extends JsonSerializer<LocalDate> {
  static DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  
  @Override
  public void serialize(LocalDate date, JsonGenerator jgen, SerializerProvider provider)
    throws IOException, JsonProcessingException {
    jgen.writeString(date.format(dtFmt));
  }
}
