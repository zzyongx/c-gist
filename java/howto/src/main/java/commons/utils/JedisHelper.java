package commons.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisHelper {
  public static String get(JedisPool pool, String key) {
    try (Jedis c = pool.getResource()) {
      return c.get(key);
    }
  }

  public static void del(JedisPool pool, String key) {
    try (Jedis c = pool.getResource()) {
      c.del(key);
    }
  }
}

