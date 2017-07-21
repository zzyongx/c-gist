package example.config;

import java.util.*;
import javax.sql.DataSource;
import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import commons.spring.SimpleTransactionTemplate;
import commons.mybatis.*;
import commons.utils.*;

@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = ProjectInfo.MAPPER_PKG, sqlSessionFactoryRef = "sqlSessionFactory")
public class DaoConfig {
  @Autowired Environment env;

  @Bean(initMethod = "init", destroyMethod = "close")
  public DataSource dataSource() {
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setDriverClassName(env.getRequiredProperty("jdbc.driver"));
    dataSource.setUrl(env.getRequiredProperty("jdbc.url"));
    dataSource.setUsername(env.getRequiredProperty("jdbc.username"));
    dataSource.setPassword(env.getRequiredProperty("jdbc.password"));
    dataSource.setMaxActive(20);
    dataSource.setInitialSize(1);
    dataSource.setMaxWait(60000);
    dataSource.setMinIdle(3);
    dataSource.setRemoveAbandoned(true);
    dataSource.setRemoveAbandonedTimeout(180);
    dataSource.setConnectionProperties("clientEncoding=UTF-8");
    dataSource.setConnectionInitSqls(Arrays.asList("set names utf8mb4;"));
    return dataSource;
  }

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
    sqlSessionFactoryBean.setDataSource(dataSource());

    TypeHandler[] handlers = new TypeHandler[] {
      new LocalDateTimeTypeHandler(),
      new LocalDateTypeHandler(),
    };
    sqlSessionFactoryBean.setTypeHandlers(handlers);

    SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) sqlSessionFactoryBean.getObject();

    org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
    configuration.setCacheEnabled(false);
    configuration.setMultipleResultSetsEnabled(true);
    configuration.setDefaultExecutorType(ExecutorType.REUSE);
    configuration.setLazyLoadingEnabled(false);
    configuration.setAggressiveLazyLoading(true);
    configuration.setDefaultStatementTimeout(300);
    configuration.setLogPrefix("dao.");

    Properties properties = new Properties();
    properties.put("sql.slowLimit", env.getProperty("sql.slowLimit", "-1"));

    SlowLogInterceptor slowLogInterceptor = new SlowLogInterceptor();
    slowLogInterceptor.setProperties(properties);
    configuration.addInterceptor(slowLogInterceptor);

    MyBatisHelper.registerEnumHandler(
      configuration.getTypeHandlerRegistry(), EnumValueTypeHandler.class, ProjectInfo.PKG_PREFIX);

    configuration.getTypeHandlerRegistry().register(new StringListTypeHandler());

    return sqlSessionFactory;
  }

  @Bean
  public DataSourceTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean SimpleTransactionTemplate simpleTransactionTemplate() {
    TransactionTemplate tt = new TransactionTemplate(transactionManager());
    return new SimpleTransactionTemplate(tt);
  }
}
