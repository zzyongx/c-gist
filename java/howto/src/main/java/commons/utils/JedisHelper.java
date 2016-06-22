package commons.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisHelper {
  public static String get(JedisPool pool, String key) {
    try (Jedis c = pool.getResource()) {
      return c.get(key);
    }
  }

  public static void set(JedisPool pool, String key, String value) {
    try (Jedis c = pool.getResource()) {
      c.set(key, value);
    }
  }

  public static void setex(JedisPool pool, String key, int timeout, String value) {
    try (Jedis c = pool.getResource()) {
      c.setex(key, timeout, value);
    }
  }

  public static void incr(JedisPool pool, String key) {
    try (Jedis c = pool.getResource()) {
      c.incr(key);
    }
  }

  public static void decr(JedisPool pool, String key) {
    try (Jedis c = pool.getResource()) {
      c.decr(key);
    }
  }

  public static void del(JedisPool pool, String key) {
    try (Jedis c = pool.getResource()) {
      c.del(key);
    }
  }

}

