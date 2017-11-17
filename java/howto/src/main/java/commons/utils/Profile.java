package commons.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Profile {
  public static final Logger logger = LoggerFactory.getLogger(Profile.class);

  private static String objectToString(Object object) {
    if (object == null) return null;

    String className = object.getClass().getName();
    if (className.equals("[B")) {
      byte[] bytes = (byte[]) object;
      return "0x" + StringHelper.toHex(bytes);
    } else if (className.equals("[I")) {
      int[] ints = (int[]) object;
      return Arrays.toString(ints);
    } else if (className.equals("[J")) {
      long[] longs = (long[]) object;
      return Arrays.toString(longs);
    } else if (className.equals("[C")) {
      char[] chars = (char[]) object;
      return Arrays.toString(chars);
    } else if (className.equals("[F")) {
      float[] floats = (float[]) object;
      return Arrays.toString(floats);
    } else if (className.equals("[D")) {
      double[] doubles = (double[]) object;
      return Arrays.toString(doubles);
    }
    return null;
  }

  public static String buildName(String namePrefix, List<Object> list) {
    if (namePrefix.startsWith(":") || list.isEmpty()) return namePrefix;

    try {
      StringBuilder builder = new StringBuilder(namePrefix);
      for (Object object : list) {
        String string = objectToString(object);
        if (string == null) string = object.toString();
        builder.append('-').append(string);
      }
      return builder.toString();
    } catch (Exception e) {   // object.toString() may throw
      logger.warn("buildName {} throws {}", namePrefix, Throwables.getStackTraceAsString(e));
      return namePrefix;
    }
  }

  public static <R, T1> R apply(int limit, String name, Class<R> rtype, Function<T1, R> f, T1 t1) {
    return apply(limit, name, Arrays.asList(t1), rtype, () -> f.apply(t1));
  }

  public static <R, T1, T2> R apply(int limit, String name, Class<R> rtype, BiFunction<T1, T2, R> f, T1 t1, T2 t2) {
    return apply(limit, name, Arrays.asList(t1, t2), rtype, () -> f.apply(t1, t2));
  }

  public static interface Function3<T1, T2, T3, R> {
    R apply(T1 t1, T2 t2, T3 t3);
  }

  public static <R, T1, T2, T3> R apply(int limit, String name, Class<R> rtype, Function3<T1, T2, T3, R> f,
                                        T1 t1, T2 t2, T3 t3) {
    return apply(limit, name, Arrays.asList(t1, t2, t3), rtype, () -> f.apply(t1, t2, t3));
  }

  public static <R> R apply(int limit, String name, Class<R> rtype, Supplier<R> supplier) {
    return apply(limit, name, Collections.emptyList(), rtype, supplier);
  }

  public static <R> R apply(int limit, String name, List<Object> args, Class<R> rtype, Supplier<R> supplier) {
    if (name.startsWith("#") || !logger.isDebugEnabled()) return supplier.get();

    long start = System.currentTimeMillis();
    R r = supplier.get();
    long consume = System.currentTimeMillis() - start;
    if (consume >= limit) logger.debug("{} consumes {}", buildName(name, args), consume);
    return r;
  }

  public static <T1> void apply(int limit, String name, Consumer<T1> f, T1 t1) {
    apply(limit, name, Arrays.asList(t1), () -> f.accept(t1));
  }

  public static <T1, T2> void apply(int limit, String name, BiConsumer<T1, T2> f, T1 t1, T2 t2) {
    apply(limit, name, Arrays.asList(t1, t2), () -> f.accept(t1, t2));
  }

  public static interface Consumer3<T1, T2, T3> {
    void accept(T1 t1, T2 t2, T3 t3);
  }

  public static <T1, T2, T3> void apply(int limit, String name, Consumer3<T1, T2, T3> f, T1 t1, T2 t2, T3 t3) {
    apply(limit, name, Arrays.asList(t1, t2, t3), () -> f.accept(t1, t2, t3));
  }

  public static void apply(int limit, String name, Runnable runnable) {
    apply(limit, name, Collections.emptyList(), runnable);
  }

  public static void apply(int limit, String name, List<Object> args, Runnable runnable) {
    if (name.startsWith("#") || !logger.isDebugEnabled()) {
      runnable.run();
    } else {
      long start = System.currentTimeMillis();
      runnable.run();
      long consume = System.currentTimeMillis() - start;
      if (true || consume > limit) logger.debug("{} comsumes {}", buildName(name, args), consume);
    }
  }
}
