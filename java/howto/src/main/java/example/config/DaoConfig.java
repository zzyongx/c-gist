package example.config;

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

@Configuration
@EnableTransactionManagement
@MapperScan("example.dao")
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
    return dataSource;
  }

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
    sqlSessionFactory.setDataSource(dataSource());

    org.apache.ibatis.session.Configuration configuration =
      new org.apache.ibatis.session.Configuration();
    configuration.setCacheEnabled(false);
    configuration.setMultipleResultSetsEnabled(true);
    configuration.setDefaultExecutorType(ExecutorType.REUSE);
    configuration.setLazyLoadingEnabled(false);
    configuration.setAggressiveLazyLoading(true);
    configuration.setDefaultStatementTimeout(300);

    TypeHandler[] handlers = new TypeHandler[] {
      new LocalDateTimeTypeHandler(),
    };
    sqlSessionFactory.setTypeHandlers(handlers);

    return (SqlSessionFactory) sqlSessionFactory.getObject();
  }

  @Bean
  public DataSourceTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }
}
