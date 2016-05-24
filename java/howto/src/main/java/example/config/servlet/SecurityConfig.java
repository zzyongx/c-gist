package example.config.servlet;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.RememberMeServices;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends commons.spring.SecurityConfig implements InitializingBean {
  @Autowired RememberMeServices rms;
  @Autowired Environment env;

  public void afterPropertiesSet() throws Exception {
    init(rms, env);
  }
}
