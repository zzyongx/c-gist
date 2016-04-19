package commons.spring;

import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public class LooseGsonHttpMessageConverter extends MappingJackson2HttpMessageConverter {
  public LooseGsonHttpMessageConverter() {
    List<MediaType> types = Arrays.asList(
      new MediaType("text", "plain", DEFAULT_CHARSET),
      new MediaType("application", "json", DEFAULT_CHARSET),
      new MediaType("application", "*+json", DEFAULT_CHARSET),
      new MediaType("application", "octet-stream", DEFAULT_CHARSET)
      );
    super.setSupportedMediaTypes(types);
  }
}
