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
@MapperScan(basePackages = ProjectInfo.MAPPER_PKG_BIG, sqlSessionFactoryRef = "bigSqlSessionFactory")
public class BigDaoConfig extends DaoConfig {
  @Autowired
  public BigDaoConfig(Environment env) {
    this.env = env;
  }

  @Bean(initMethod = "init", destroyMethod = "close")
  public DataSource bigDataSource() {
    return dataSource("big");
  }

  @Bean
  public SqlSessionFactory bigSqlSessionFactory() throws Exception {
    return sqlSessionFactory(bigDataSource());
  }

  @Bean
  public DataSourceTransactionManager bigTransactionManager() {
    return new DataSourceTransactionManager(bigDataSource());
  }

  @Bean
  public SimpleTransactionTemplate bigTransactionTemplate() {
    TransactionTemplate tt = new TransactionTemplate(bigTransactionManager());
    return new SimpleTransactionTemplate(tt);
  }
}
