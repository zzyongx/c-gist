package example.config;

import java.util.*;
import javax.sql.DataSource;
import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import example.mapper.*;
import example.utils.*;

@Configuration
@EnableTransactionManagement
public class AdvanceDaoConfig {
  @Autowired Environment env;

  @SuppressWarnings("unchecked")
  void registerEnumValueHandler(TypeHandlerRegistry register, String packageName) {
    List<Class<? extends Enum<?>>> enums = ReflectUtil.findEnums(packageName);
    for (Class<? extends Enum<?>> clazz : enums) {
      try {
        clazz.getMethod("getValue");
      } catch (Exception e) {
        continue;
      }
      // we must appoint JavaType
      register.register(clazz, new EnumValueTypeHandler(clazz));
    }
  }

  /* for demonation horizontal partition */
  @Bean(name = "dbHorizontalPartitionDataSourceList")
  public ArrayList<DataSource> dbHorizontalPartitionDataSourceList() {
    ArrayList<DataSource> list = new ArrayList<>();
    
    for (int i = 0; i < Integer.MAX_VALUE; ++i) {
      String index = String.valueOf(i);
      String dbDriver   = env.getProperty("jdbc.driver." + index);
      String dbUrl      = env.getProperty("jdbc.url." + index);
      String dbUser     = env.getProperty("jdbc.username." + index);
      String dbPassword = env.getProperty("jdbc.password." + index);

      if (dbDriver == null || dbUrl == null || dbUser == null || dbPassword == null) {
        break;
      }
      
      DruidDataSource dataSource = new DruidDataSource();
      dataSource.setDriverClassName(env.getRequiredProperty("jdbc.driver"));
      dataSource.setUrl(env.getRequiredProperty("jdbc.url"));
      dataSource.setUsername(env.getRequiredProperty("jdbc.username"));
      dataSource.setPassword(env.getRequiredProperty("jdbc.password"));

      list.add(dataSource);
    }

    return list;
  }

  @Bean(name = "dbHorizontalPartitionSqlSessionFactory")
  public ArrayList<SqlSessionFactory> dbHorizontalPartitionSqlSessionFactory() throws Exception {
    ArrayList<DataSource> dataSourceList = dbHorizontalPartitionDataSourceList();
    ArrayList<SqlSessionFactory> sqlSessionFactoryList = new ArrayList<>();
    
    for (DataSource dataSource : dataSourceList) {
      SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
      sqlSessionFactoryBean.setDataSource(dataSource);

      TypeHandler[] handlers = new TypeHandler[] {
        new LocalDateTimeTypeHandler(),
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
      configuration.addMapper(EmployeeMapper.class);
      
      registerEnumValueHandler(configuration.getTypeHandlerRegistry(), ProjectInfo.PKG_PREFIX);

      sqlSessionFactoryList.add(sqlSessionFactory);
    }

    return sqlSessionFactoryList;
  }

  @Bean(name = "dbHorizontalPartition")
  public ArrayList<EmployeeMapper> dbHorizontalPartition() throws Exception {
    ArrayList<SqlSessionFactory> sqlSessionFactoryList = dbHorizontalPartitionSqlSessionFactory();
    ArrayList<EmployeeMapper> employeeMapperList = new ArrayList<>();

    for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
      SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
      employeeMapperList.add(sqlSessionTemplate.getMapper(EmployeeMapper.class));
    }
    
    return employeeMapperList;
  }

  @Bean(name = "dbHorizontalPartitionTransactionManager")
  public ArrayList<DataSourceTransactionManager> transactionManager() {
    ArrayList<DataSource> dataSourceList = dbHorizontalPartitionDataSourceList();
    ArrayList<DataSourceTransactionManager> txManagerList = new ArrayList<>();
    for (DataSource dataSource : dataSourceList) {
      txManagerList.add(new DataSourceTransactionManager(dataSource));
    }
    return txManagerList;
  }

  /*
  @Bean
  public MapperScannerConfigurer mapperScannerConfigurer1() {
    MapperScannerConfigurer configurer = new MapperScannerConfigurer();
    configurer.setBasePackage(MAPPERS_PACKAGE_NAME_1);
    configurer.setSqlSessionFactoryBeanName(SQL_SESSION_FACTORY_NAME_1);
    return configurer;
  }
  */
}
