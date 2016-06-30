package commons.spring;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.AuthenticationEntryPoint;

class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
  static final String s401 = "{`code`: 401, `message`: `Unauthorized`}".replace('`', '"');
  
  @Override
  public void commence(
    HttpServletRequest request, HttpServletResponse response,
    AuthenticationException authException) throws IOException {

    response.setContentType("application/json");
    response.getOutputStream().print(s401);
  }
}

public class SecurityConfigWithRedisRememberMeService extends WebSecurityConfigurerAdapter {
  private RememberMeServices rms;
  private Environment        env;

  public void init(RememberMeServices rms, Environment env) {
    this.rms = rms;
    this.env = env;
  }

  void configSitePermit(HttpSecurity http) throws Exception {
    http.authorizeRequests().antMatchers("/**").authenticated();
  }

  boolean configUrlPermit(HttpSecurity http, boolean whiteList) throws Exception {
    String config = whiteList ? "url.permit." : "url.deny.";
    boolean hasConfig = false;
    
    for (int i = 1; i < 101; ++i) {
      String request = env.getProperty(config + String.valueOf(i));
      if (request == null) continue;

      hasConfig = true;

      String parts[] = request.split(":", 2);
      HttpMethod method = null;
      String url;
      if (parts.length == 1) {
        url = request;
      } else {
        url = parts[1];
        if (parts[0].equalsIgnoreCase("get")) {
          method = HttpMethod.GET;
        } else if (parts[0].equalsIgnoreCase("post")) {
          method = HttpMethod.POST;
        } else if (parts[0].equalsIgnoreCase("put")) {
          method = HttpMethod.PUT;
        } else if (parts[0].equalsIgnoreCase("delete")) {
          method = HttpMethod.DELETE;
        }
      }

      if (whiteList) {
        if (method != null) {
          http.authorizeRequests().antMatchers(method, url).permitAll();
        } else {
          http.authorizeRequests().antMatchers(url).permitAll();
        }
      } else {
        if (method != null) {
          http.authorizeRequests().antMatchers(method, url).authenticated();
        } else {
          http.authorizeRequests().antMatchers(url).authenticated();
        }
      }
    }

    if (hasConfig) {
      if (whiteList) {
        http.authorizeRequests().anyRequest().authenticated();
      } else {
        http.authorizeRequests().anyRequest().permitAll();
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    http.exceptionHandling()
      .authenticationEntryPoint(new RestAuthenticationEntryPoint());

    if (!configUrlPermit(http, true)) {
      if (!configUrlPermit(http, false)) {
        configSitePermit(http);
      }
    }

    http.rememberMe().rememberMeServices(rms);
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    RememberMeAuthenticationProvider p = new RememberMeAuthenticationProvider("N/A");
    auth.authenticationProvider(p);
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }
}
