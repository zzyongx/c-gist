package commons.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.Kryo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisCacheHelper {
  public static final Logger logger = LoggerFactory.getLogger(JedisCacheHelper.class);

  public static String buildCacheKey(String cachePrefix, List<Object> list) {
    if (cachePrefix.startsWith(":") || list.isEmpty()) return cachePrefix;

    try {
      StringBuilder builder = new StringBuilder(cachePrefix);
      for (Object object : list) {
        builder.append('-').append(object == null ? "" : object.toString());
      }
      return builder.toString();
    } catch (Exception e) {   // object.toString() may throw
      logger.warn("buildCacheKey {} throws {}", cachePrefix, Throwables.getStackTraceAsString(e));
      return null;
    }
  }

  public static <R, T1> R apply(
    JedisPool pool, int expire, String cachePrefix, Class<R> rtype,
    Function<T1, R> f, T1 t1) {
    String cacheKey = buildCacheKey(cachePrefix, Arrays.asList(t1));
    return apply(pool, expire, cacheKey, rtype, () -> f.apply(t1));
  }

  public static <R, T1> List<R> applyList(
    JedisPool pool, int expire, String cachePrefix, Class<R> rtype,
    Function<T1, List<R>> f, T1 t1) {
    String cacheKey = buildCacheKey(cachePrefix, Arrays.asList(t1));
    return applyList(pool, expire, cacheKey, rtype, () -> f.apply(t1));
  }

  public static <R, T1, T2> R apply(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    BiFunction<T1, T2, R> f, T1 t1, T2 t2) {
    String cacheKey = buildCacheKey(cachePrefix, Arrays.asList(t1, t2));
    return apply(jedisPool, expire, cacheKey, rtype, () -> f.apply(t1, t2));
  }

  public static <R, T1, T2> List<R> applyList(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    BiFunction<T1, T2, List<R>> f, T1 t1, T2 t2) {
    String cacheKey = buildCacheKey(cachePrefix, Arrays.asList(t1, t2));
    return applyList(jedisPool, expire, cacheKey, rtype, () -> f.apply(t1, t2));
  }

  public static interface Function3<T1, T2, T3, R> {
    R apply(T1 t1, T2 t2, T3 t3);
  }

  public static <R, T1, T2, T3> R apply(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    Function3<T1, T2, T3, R> f, T1 t1, T2 t2, T3 t3) {
    String cacheKey = buildCacheKey(cachePrefix, Arrays.asList(t1, t2, t3));
    return apply(jedisPool, expire, cacheKey, rtype, () -> f.apply(t1, t2, t3));
  }

  public static <R, T1, T2, T3> List<R> applyList(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    Function3<T1, T2, T3, List<R>> f, T1 t1, T2 t2, T3 t3) {
    String cacheKey = buildCacheKey(cachePrefix, Arrays.asList(t1, t2, t3));
    return applyList(jedisPool, expire, cacheKey, rtype, () -> f.apply(t1, t2, t3));
  }

  private static <R> R bytesToObject(byte[] bytes, Class<R> rtype) {
    Kryo kryo = new Kryo();
    Input input = new Input(new ByteArrayInputStream(bytes));
    R r = kryo.readObject(input, rtype);
    input.close();
    return r;
  }

  private static <R> List<R> bytesToObjectList(byte[] bytes, Class<R> rtype) {
    Kryo kryo = new Kryo();
    Input input = new Input(new ByteArrayInputStream(bytes));

    List<R> list = new ArrayList<>();
    while (!input.eof()) {
      R r = kryo.readObjectOrNull(input, rtype);
      list.add(r);
    }

    input.close();
    return list;
  }

  private static <R> byte[] objectToBytes(R r) {
    Kryo kryo = new Kryo();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Output output = new Output(out);
    kryo.writeObject(output, r);
    output.close();
    return out.toByteArray();
  }

  private static <R> byte[] objectListToBytes(List<R> list, Class<R> rtype) {
    Kryo kryo = new Kryo();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Output output = new Output(out);
    for (R r : list) kryo.writeObjectOrNull(output, r, rtype);
    output.close();
    return out.toByteArray();
  }

  public static <R> R apply(JedisPool pool, int expire, String cacheKey, Class<R> rtype, Supplier<R> supplier) {
    // build cache key fail, call data supplier
    if (cacheKey == null) return supplier.get();

    byte[] value = null;
    try (Jedis c = pool.getResource()) {
      value = c.get(cacheKey.getBytes(StandardCharsets.UTF_8));
      if (value != null) {
        R r = bytesToObject(value, rtype);
        logger.info("cache hits {}", cacheKey);
        return r;
      } else {
        logger.info("cache miss {}", cacheKey);
      }
    } catch (Exception e) {
      logger.warn("getCache {} throws {}", cacheKey, Throwables.getStackTraceAsString(e));
    }

    R r = supplier.get();
    if (r == null) return r;

    try (Jedis c = pool.getResource()) {
      if (expire <= 0) expire = 864000;
      c.setex(cacheKey.getBytes(StandardCharsets.UTF_8), expire, objectToBytes(r));
    } catch (Exception e) {
      logger.warn("setCache {} throws {}", cacheKey, Throwables.getStackTraceAsString(e));
    }
    return r;
  }

  public static <R> List<R> applyList(JedisPool pool, int expire, String cacheKey, Class<R> rtype,
                                      Supplier<List<R>> supplier) {
    // build cache key fail, call data supplier
    if (cacheKey == null) return supplier.get();

    byte[] value = null;
    try (Jedis c = pool.getResource()) {
      value = c.get(cacheKey.getBytes(StandardCharsets.UTF_8));
      if (value != null) {
        List<R> r = bytesToObjectList(value, rtype);
        logger.info("cache hits {}", cacheKey);
        return r;
      } else {
        logger.info("cache miss {}", cacheKey);
      }
    } catch (Exception e) {
      logger.warn("getCache {} throws {}", cacheKey, Throwables.getStackTraceAsString(e));
    }

    List<R> r = supplier.get();
    if (r == null) return r;

    try (Jedis c = pool.getResource()) {
      if (expire <= 0) expire = 864000;
      c.setex(cacheKey.getBytes(StandardCharsets.UTF_8), expire, objectListToBytes(r, rtype));
    } catch (Exception e) {
      logger.warn("setCache {} throws {}", cacheKey, Throwables.getStackTraceAsString(e));
    }
    return r;
  }
}
