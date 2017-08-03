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
@MapperScan(basePackages = ProjectInfo.MAPPER_PKG_DATA, sqlSessionFactoryRef = "dataSqlSessionFactory")
public class DataDaoConfig extends DaoConfig {
  @Autowired
  public DataDaoConfig(Environment env) {
    this.env = env;
  }

  @Bean(initMethod = "init", destroyMethod = "close")
  public DataSource dataDataSource() {
    return dataSource("data");
  }

  @Bean
  public SqlSessionFactory dataSqlSessionFactory() throws Exception {
    return sqlSessionFactory(dataDataSource());
  }

  @Bean
  public DataSourceTransactionManager dataTransactionManager() {
    return new DataSourceTransactionManager(dataDataSource());
  }

  @Bean
  public SimpleTransactionTemplate dataTransactionTemplate() {
    TransactionTemplate tt = new TransactionTemplate(dataTransactionManager());
    return new SimpleTransactionTemplate(tt);
  }
}
