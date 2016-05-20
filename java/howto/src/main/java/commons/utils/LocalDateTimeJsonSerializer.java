package commons.utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class LocalDateTimeJsonSerializer extends JsonSerializer<LocalDateTime> {
  static DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  
  @Override
  public void serialize(LocalDateTime dateTime, JsonGenerator jgen, SerializerProvider provider)
    throws IOException, JsonProcessingException {
    jgen.writeString(dateTime.format(dtFmt));
  }
}
