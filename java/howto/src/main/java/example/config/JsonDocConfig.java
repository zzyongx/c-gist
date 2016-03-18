package example.config;

import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.jsondoc.springmvc.controller.JSONDocController;

@Configuration
public class JsonDocConfig {
  @Bean(name = "documentationController")
  public JSONDocController jsonDocController() {
    return new JSONDocController(
      "1.0", "",
      Arrays.asList("example.api", "example.entity"));
  }
}
