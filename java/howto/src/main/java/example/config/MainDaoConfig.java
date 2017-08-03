package example.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import commons.spring.SimpleTransactionTemplate;

@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = ProjectInfo.MAPPER_PKG_MAIN, sqlSessionFactoryRef = "mainSqlSessionFactory")
public class MainDaoConfig extends DaoConfig {
  @Autowired
  public MainDaoConfig(Environment env) {
    this.env = env;
  }

  @Bean(initMethod = "init", destroyMethod = "close")
  public DataSource mainDataSource() {
    return dataSource("main");
  }

  @Bean
  public SqlSessionFactory mainSqlSessionFactory() throws Exception {
    return sqlSessionFactory(mainDataSource());
  }

  @Bean
  public DataSourceTransactionManager mainTransactionManager() {
    return new DataSourceTransactionManager(mainDataSource());
  }

  @Bean
  public SimpleTransactionTemplate mainTransactionTemplate() {
    TransactionTemplate tt = new TransactionTemplate(mainTransactionManager());
    return new SimpleTransactionTemplate(tt);
  }
}
