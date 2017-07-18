package commons.mybatis;

import java.lang.reflect.Method;
import java.util.Properties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({
    @Signature(type= Executor.class,
               method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class,
               method = "update",
               args = {MappedStatement.class, Object.class})}
)
public class SlowLogInterceptor implements Interceptor {
  private static final Logger logger = LoggerFactory.getLogger(SlowLogInterceptor.class);
  private int slowLimit = -1;

  private String getSql(String method, Object args[]) {
    try {
      MappedStatement ms = (MappedStatement) args[0];
      Object param       = args[1];

      BoundSql boundSql = ms.getBoundSql(param);
      return boundSql.getSql().replace('\n', ' ') + " WITHPARAM " + param;
    } catch (Exception e) {
      logger.error("SlowLogInterceptor", e);
    }
    return null;
  }

  public Object intercept(Invocation invocation) throws Throwable {
    if (slowLimit < 0) return invocation.proceed();

    long start = System.currentTimeMillis();
    Object object = invocation.proceed();
    long end = System.currentTimeMillis();

    if (end - start >= slowLimit) {
      Method method = invocation.getMethod();
      String sql = getSql(method.getName(), invocation.getArgs());
      if (sql != null) {
        logger.info("{} COMSUMES {}", sql, end - start);
      }
    }
    return object;
  }

  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  public void setProperties(Properties properties) {
    this.slowLimit = Integer.parseInt(properties.getProperty("sql.slowLimit", "-1"));
  }
}
