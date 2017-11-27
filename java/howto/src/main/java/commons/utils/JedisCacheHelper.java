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
  private static final byte[] PH = new byte[] {'1'};

  public static String buildCacheField(List<Object> list) {
    try {
      StringBuilder builder = new StringBuilder();
      boolean first = true;
      for (Object object : list) {
        if (!first) builder.append('-');
        builder.append(object == null ? "" : object.toString());
        first = false;
      }
      return builder.toString();
    } catch (Exception e) {   // object.toString() may throw
      logger.warn("buildCacheField throws {}", Throwables.getStackTraceAsString(e));
      return null;
    }
  }

  public static <R> R apply(
    JedisPool pool, int expire, String cacheKey, Class<R> rtype, Supplier<R> supplier) {
    return apply(pool, expire, cacheKey, null, rtype, supplier);
  }

  public static <R> List<R> applyList(
    JedisPool pool, int expire, String cacheKey, Class<R> rtype, Supplier<List<R>> supplier) {
    return applyList(pool, expire, cacheKey, null, rtype, supplier);
  }

  public static <R, T1> R apply(
    JedisPool pool, int expire, String cachePrefix, Class<R> rtype,
    Function<T1, R> f, T1 t1) {
    return apply(pool, expire, cachePrefix, Arrays.asList(t1), rtype, () -> f.apply(t1));
  }

  public static <R, T1> List<R> applyList(
    JedisPool pool, int expire, String cachePrefix, Class<R> rtype,
    Function<T1, List<R>> f, T1 t1) {
    return applyList(pool, expire, cachePrefix, Arrays.asList(t1), rtype, () -> f.apply(t1));
  }

  public static <R, T1, T2> R apply(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    BiFunction<T1, T2, R> f, T1 t1, T2 t2) {
    return apply(jedisPool, expire, cachePrefix, Arrays.asList(t1, t2), rtype, () -> f.apply(t1, t2));
  }

  public static <R, T1, T2> List<R> applyList(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    BiFunction<T1, T2, List<R>> f, T1 t1, T2 t2) {
    return applyList(jedisPool, expire, cachePrefix, Arrays.asList(t1, t2), rtype, () -> f.apply(t1, t2));
  }

  public static interface Function3<T1, T2, T3, R> {
    R apply(T1 t1, T2 t2, T3 t3);
  }

  public static <R, T1, T2, T3> R apply(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    Function3<T1, T2, T3, R> f, T1 t1, T2 t2, T3 t3) {
    return apply(jedisPool, expire, cachePrefix, Arrays.asList(t1, t2, t3), rtype, () -> f.apply(t1, t2, t3));
  }

  public static <R, T1, T2, T3> List<R> applyList(
    JedisPool jedisPool, int expire, String cachePrefix, Class<R> rtype,
    Function3<T1, T2, T3, List<R>> f, T1 t1, T2 t2, T3 t3) {
    return applyList(jedisPool, expire, cachePrefix, Arrays.asList(t1, t2, t3), rtype, () -> f.apply(t1, t2, t3));
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

  private static class CacheKey {
    public byte[] mkey       = null;
    public String mkeyString = null;

    public byte[] key   = null;
    public byte[] field = null;
  }

  private static CacheKey buildCacheKey(String cachePrefix, List<Object> args) {
    if (cachePrefix.startsWith("#")) return null;

    CacheKey cacheKey = new CacheKey();
    if (cachePrefix.startsWith(":") || args == null) {
      cacheKey.mkey = cachePrefix.getBytes(StandardCharsets.UTF_8);
      cacheKey.mkeyString = cachePrefix;
    } else {
      String fieldStr = buildCacheField(args);
      if (fieldStr == null) return null;

      cacheKey.mkeyString = cachePrefix + "-" + fieldStr;
      cacheKey.mkey = cacheKey.mkeyString.getBytes(StandardCharsets.UTF_8);

      cacheKey.key   = cachePrefix.getBytes(StandardCharsets.UTF_8);
      cacheKey.field = fieldStr.getBytes(StandardCharsets.UTF_8);
    }

    return cacheKey;
  }

  public static byte[] buildCacheKeyFromKeyField(byte[] key, byte[] field) {
    int idx = 0;
    byte[] mkey = new byte[key.length + field.length + 1];

    for (int i = 0; i < key.length; ++i) mkey[idx++] = key[i];
    mkey[idx++] = '-';
    for (int i = 0; i < field.length; ++i) mkey[idx++] = field[i];

    return mkey;
  }

  public static <R> R apply(JedisPool pool, int expire, String cachePrefix, List<Object> args,
                            Class<R> rtype, Supplier<R> supplier) {
    CacheKey cacheKey = buildCacheKey(cachePrefix, args);
    if (cacheKey == null) return supplier.get();

    byte[] value = null;
    try (Jedis c = pool.getResource()) {
      value = c.get(cacheKey.mkey);

      if (value != null) {
        R r = bytesToObject(value, rtype);
        logger.debug("cache hits {}", cacheKey.mkeyString);
        return r;
      } else {
        if (cacheKey.key != null) c.hdel(cacheKey.key, cacheKey.field);
        logger.debug("cache miss {}", cacheKey.mkeyString);
      }
    } catch (Exception e) {
      logger.warn("getCache {} throws {}", cacheKey.mkeyString, Throwables.getStackTraceAsString(e));
    }

    R r = supplier.get();
    if (r == null) return r;

    try (Jedis c = pool.getResource()) {
      if (expire <= 0) expire = 864000;
      c.setex(cacheKey.mkey, expire, objectToBytes(r));

      if (cacheKey.key != null) c.hset(cacheKey.key, cacheKey.field, PH);
    } catch (Exception e) {
      logger.warn("setCache {} throws {}", cacheKey.mkeyString, Throwables.getStackTraceAsString(e));
    }
    return r;
  }

  public static <R> List<R> applyList(JedisPool pool, int expire, String cachePrefix, List<Object> args,
                                      Class<R> rtype, Supplier<List<R>> supplier) {
    CacheKey cacheKey = buildCacheKey(cachePrefix, args);
    if (cacheKey == null) return supplier.get();

    byte[] value = null;
    try (Jedis c = pool.getResource()) {
      value = c.get(cacheKey.mkey);

      if (value != null) {
        List<R> r = bytesToObjectList(value, rtype);
        logger.debug("cache hits {}", cacheKey.mkeyString);
        return r;
      } else {
        if (cacheKey.key != null) c.hdel(cacheKey.key, cacheKey.field);
        logger.debug("cache miss {}", cacheKey.mkeyString);
      }
    } catch (Exception e) {
      logger.warn("getCache {} throws {}", cacheKey.mkeyString, Throwables.getStackTraceAsString(e));
    }

    List<R> r = supplier.get();
    if (r == null) return r;

    try (Jedis c = pool.getResource()) {
      if (expire <= 0) expire = 864000;
      c.setex(cacheKey.mkey, expire, objectListToBytes(r, rtype));

      if (cacheKey.key != null) c.hset(cacheKey.key, cacheKey.field, PH);
    } catch (Exception e) {
      logger.warn("setCache {} throws {}", cacheKey.mkeyString, Throwables.getStackTraceAsString(e));
    }
    return r;
  }
}
