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
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import commons.mybatis.*;
import commons.utils.*;
import example.mapper.*;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
public class SmartDaoConfig {
  @Autowired Environment env;
  
  @Bean(name = "onlyOneDataSource")
  public DataSource dataSource() {
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setDriverClassName(env.getRequiredProperty("jdbc.driver"));
    dataSource.setUrl(env.getRequiredProperty("jdbc.url"));
    dataSource.setUsername(env.getRequiredProperty("jdbc.username"));
    dataSource.setPassword(env.getRequiredProperty("jdbc.password"));
    return dataSource;
  }

  @Bean(name = "smartDataSource")
  public DataSource smartDataSource() {
    Map<Object, Object> map = new HashMap<>();
    map.put("master", dataSource());
    map.put("slave-1", dataSource());
    map.put("slave-2", dataSource());
    
    SmartDataSource smartDataSource = new SmartDataSource(2);
    smartDataSource.setTargetDataSources(map);
    smartDataSource.setDefaultTargetDataSource(dataSource());
    return smartDataSource;
  }
    
  @Bean(name = "onlyOneSqlSessionFactory")
  public SqlSessionFactory smartSqlSessionFactory() throws Exception {
    SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
    sqlSessionFactoryBean.setDataSource(smartDataSource());

    SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) sqlSessionFactoryBean.getObject();

    org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
    configuration.setCacheEnabled(false);
    configuration.setMultipleResultSetsEnabled(true);
    configuration.setDefaultExecutorType(ExecutorType.REUSE);
    configuration.setLazyLoadingEnabled(false);
    configuration.setAggressiveLazyLoading(true);
    configuration.setDefaultStatementTimeout(300);
    configuration.addMapper(EmployeeMapper.class);
      
    MyBatisHelper.registerEnumHandler(
      configuration.getTypeHandlerRegistry(), EnumValueTypeHandler.class, ProjectInfo.PKG_PREFIX);

    return sqlSessionFactory;
  }

  @Bean(name = "smartEmployeeMapper")
  public EmployeeMapper smartEmployeeMapper() throws Exception {
    return new SqlSessionTemplate(smartSqlSessionFactory()).getMapper(EmployeeMapper.class);
  }  
  
  @Bean(name = "onlyOneTransactionManager")
  public DataSourceTransactionManager smartTransactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public Advisor smartDataSourceAdvisor() {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression("execution(* " + ProjectInfo.MAPPER_PKG + ".*.*(..))");
    return new DefaultPointcutAdvisor(pointcut, new SmartDataSource.MapperAdvice());
  }
}
