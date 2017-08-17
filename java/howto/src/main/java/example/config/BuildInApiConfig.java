package example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import commons.jsondoc.*;

@Configuration
public class BuildInApiConfig {
  @Bean(name = "documentationController")
  public JsonDocController jsonDocController() {
    JsonDocController c = new JsonDocController("1.0", "", ProjectInfo.DOC_PKG);
    c.setJsonDocScanner(new Spring4xJSONDocScanner());
    return c;
  }
}
