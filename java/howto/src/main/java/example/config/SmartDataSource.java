package example.config;

import java.util.LinkedList;
import java.lang.reflect.*;
import java.lang.annotation.*;
import org.apache.ibatis.annotations.*;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.aop.MethodBeforeAdvice;
import example.annotation.PartitionKey;

public class SmartDataSource extends AbstractRoutingDataSource {
  public static class ContextHolder {
    private static final ThreadLocal<LinkedList<String>> debug =
      ThreadLocal.withInitial(() -> {return new LinkedList<String>();});
    
    private static final ThreadLocal<String> strHolder =
      new ThreadLocal<String>();
    private static final ThreadLocal<Long> longHolder =
      new ThreadLocal<Long>();
    
    public static String get(int m) {
      String s = strHolder.get();
      strHolder.remove();
      
      Long l = longHolder.get();
      if (l != null) {
        l %= m;
        longHolder.remove();
        s = s + "-" + String.valueOf(l);
      }

      debug.get().push(s);
      if (debug.get().size() > 10) debug.get().pop();
      
      return s;
    }

    public static void set(String s) {
      set(s, -1);
    }
    
    public static void set(String s, long l) {
      strHolder.set(s);
      if (l != -1) longHolder.set(l);
    }

    public static LinkedList<String> getDebug() {
      return debug.get();
    }
  }

  public static class MapperAdvice implements MethodBeforeAdvice {
    public void before(Method method, Object[] args, Object target) throws Exception {
      if (!method.isAnnotationPresent(Select.class) &&
          !method.isAnnotationPresent(SelectProvider.class)) {
        ContextHolder.set("master");
      } else {
        Class argTyps[] = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();

        long id;
        boolean found = false;
        for (int i = 0; i < args.length && !found; ++i) {
          for (int j = 0; j < annotations[i].length && !found; ++j) {
            if (annotations[i][j].annotationType().isAssignableFrom(PartitionKey.class)) {
              PartitionKey pk = (PartitionKey) annotations[i][j];
              if (argTyps[i].isPrimitive()) {
                id = (long) args[i];
              } else {
                Method findIdMethod = argTyps[i].getMethod("id");
                id = (long) findIdMethod.invoke(args[i]);
              }
              ContextHolder.set("slave", id);
              found = true;
            }
          }
        }
      }
    }
  }

  private int slaveNumber;

  public SmartDataSource(int slaveNumber) {
    this.slaveNumber = slaveNumber;
  }
  
  @Override
  protected Object determineCurrentLookupKey() {
    return ContextHolder.get(slaveNumber);
  }
}
