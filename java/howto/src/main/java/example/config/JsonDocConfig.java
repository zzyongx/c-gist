package example.config;

import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import commons.jsondoc.Spring4xJSONDocScanner;
import commons.jsondoc.JsonDocController;

@Configuration
public class JsonDocConfig {
  @Bean(name = "documentationController")
  public JsonDocController jsonDocController() {
    JsonDocController c = new JsonDocController("1.0", "", ProjectInfo.DOC_PKG);
    c.setJsonDocScanner(new Spring4xJSONDocScanner());
    return c;
  }
}
