package example.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.RegistrationPolicy;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import commons.spring.*;
import commons.saas.RestNameService;

@Configuration
@EnableScheduling
@ComponentScan({"commons.spring.controller", ProjectInfo.PKG_PREFIX + ".api", ProjectInfo.PKG_PREFIX + ".manager"})
@PropertySource(value = "classpath:application-default.properties", ignoreResourceNotFound = true)
@PropertySource("classpath:application-${spring.profiles.active}.properties")
public class RootConfig {
  @Autowired Environment   env;
  @Autowired MainDaoConfig mainDaoConfig;

  @Bean
  public CountLogger countLogger() {
    return CountLogger.register(env.getProperty("ops.counter"));
  }

  @Bean
  public MBeanServerFactoryBean mbeanServer() {
    MBeanServerFactoryBean mbeanServer = new MBeanServerFactoryBean();
    mbeanServer.setDefaultDomain(ProjectInfo.PKG_PREFIX);
    mbeanServer.setLocateExistingServerIfPossible(true);
    return mbeanServer;
  }

  @Bean
  public AnnotationMBeanExporter annotationMBeanExporter() {
    AnnotationMBeanExporter exporter = new AnnotationMBeanExporter() {
      @Override
      protected ObjectName getObjectName(Object bean, String beanKey)
        throws MalformedObjectNameException {

        ObjectName objName = super.getObjectName(bean, beanKey);

        Hashtable<String, String> ht = objName.getKeyPropertyList();
        if ("counter".equals(ht.get("name"))) ht.put("type", beanKey);

        return ObjectName.getInstance(ProjectInfo.PKG_PREFIX, ht);
      }
    };
    exporter.setRegistrationPolicy(RegistrationPolicy.IGNORE_EXISTING);
    exporter.setBeans(countLogger().getBeans());
    return exporter;
  }

  @Bean
  public LoggerFilter loggerFilter() {
    return new LoggerFilter(env);
  }

  @Bean
  public XssFilter xssFilter() {
    return new XssFilter(env);
  }

  @Bean
  public JedisPool jedisPool() {
    return new JedisPool(
      new JedisPoolConfig(),
      env.getRequiredProperty("redis.url"),
      env.getRequiredProperty("redis.port", Integer.class),
      200,  // default timeout 2000 millisecond is too long, set to 200
      env.getProperty("redis.pass"));  // if null, ignore pass
  }

  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(Integer.parseInt(env.getProperty("threadpool.max", "1")));
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(env.getProperty("threadpool.maxwait", Integer.class, 300));
    scheduler.setErrorHandler(new ThreadPoolTaskSchedulerErrorHandler());
    return scheduler;
  }

  @Bean
  public RestNameService restNameService() {
    return new RestNameService(env);
  }

  @Bean
  public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory =
      new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(Integer.parseInt(env.getProperty("rest.timeout.connect", "1000")));
    factory.setReadTimeout(Integer.parseInt(env.getProperty("rest.timeout.read", "10000")));

    RestTemplateFilter filter = new RestTemplateFilter();
    filter.replaceHeader("Accept", Arrays.asList("*/*"));

    RestTemplate rest = new RestTemplate(factory);
    rest.setInterceptors(Arrays.asList(filter));
    rest.getMessageConverters().add(new LooseGsonHttpMessageConverter());

    return rest;
  }

  // TAG:RememberMeService
  @Bean
  public RedisRememberMeService rememberMeServices() throws IOException {
    RedisRememberMeService rms = new RedisRememberMeService(
      jedisPool(), env.getProperty("rest.tokenpool", ""),
      env.getProperty("rest.inner", Boolean.class, false),
      env.getProperty("rest.anonymous", ""));
    rms.setCookiePrefix(env.getProperty("login.cookieprefix", ""));
    rms.setApiAuthDb(mainDaoConfig.mainDataSource(), env.getProperty("login.apiauth.sql"));
    rms.setPermException(env.getProperty("login.perm.exception"));
    rms.setSuPermId(env.getProperty("login.su", Integer.class, -1));

    String keyFile = env.getProperty("login.cookie.keyfile");
    if (keyFile != null) rms.setAesKey(new ClassPathResource(keyFile).getFile().getPath());
    return rms;
  }
}
