package commons.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.lang.reflect.Method;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.dao.DuplicateKeyException;

public class MyBatisHelper {
  @SuppressWarnings("unchecked")
  public static <T> void registerEnumHandler(
    TypeHandlerRegistry register, Class handlerType, String packageName) {
    List<Class<? extends Enum<?>>> enums = ReflectUtil.findEnums(packageName);
    for (Class<? extends Enum<?>> clazz : enums) {
      try {
        Method method = clazz.getMethod("getValue");
        if (method.getReturnType() != int.class) continue;
      } catch (Exception e) {
        continue;
      }
      try {
        TypeHandler typeHandler = (TypeHandler) handlerType
          .getConstructor(Class.class).newInstance(clazz);
        // we must appoint JavaType
        register.register(clazz, typeHandler);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static String inClauseForList(int size, String name) {
    StringBuilder builder = new StringBuilder();
    builder.append("(");

    for (int i = 0; i < size; ++i) {
      if (i != 0) builder.append(",");
      builder.append("#{").append(name).append("[").append(i).append("]}");
    }

    builder.append(")");
    return builder.toString();
  }

  public static <R> R catchDuplicateKey(Callable<R> callable, ApiResult rc) {
    try {
      return callable.call();
    } catch (RuntimeException e) {
      if (e.getClass().isAssignableFrom(DuplicateKeyException.class)) throw rc.toException();
      else throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
