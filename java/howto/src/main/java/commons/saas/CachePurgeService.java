package commons.saas;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import commons.utils.JedisCacheHelper;
import commons.utils.JedisHelper;
import commons.utils.StreamHelper;
import commons.utils.ReflectUtil;

public class CachePurgeService {
  private static Logger logger = LoggerFactory.getLogger(CachePurgeService.class);

  private JedisPool defaultJedisPool;
  private Map<String, JedisPool> jedisPoolMap = new HashMap<>();
  private Map<String, Set<String>> keySet = new HashMap<>();
  private Map<String, byte[][]> keyList = new HashMap<>();

  public CachePurgeService(JedisPool defaultJedisPool, Object ... args) {
    this.defaultJedisPool = defaultJedisPool;

    for (int i = 0; i < args.length; i+=2) {
      String key = (String) args[i];
      JedisPool pool = (JedisPool) args[i+1];
      jedisPoolMap.put(key, pool);
    }
  }

  public boolean init(String packageName) {
    List<CacheRegister> list = ReflectUtil.findAnnotations(packageName, CacheRegister.class);

    for (CacheRegister cacheRegister : list) {
      String lastKey = null;
      for (String arg : cacheRegister.value()) {
        if (arg.startsWith("*")) {
          lastKey = arg.substring(1);
        } else if (lastKey != null) {
          keySet.computeIfAbsent(lastKey, k -> new HashSet<>()).add(arg);
        }
      }
    }

    keyList = StreamHelper.toMap(
      keySet.entrySet().stream(), x -> x.getKey(),
      x -> {
        byte[][] bytes = new byte[x.getValue().size()][];
        int idx = 0;
        for (String key : x.getValue()) {
          bytes[idx++] = key.getBytes(StandardCharsets.UTF_8);
        }
        return bytes;
      });

    System.out.println(keySet);
    return true;
  }

  public boolean purge(String keyPrefix) {
    JedisPool pool = jedisPoolMap.get(keyPrefix);
    if (pool == null) pool = defaultJedisPool;

    byte[][] keys = keyList.get(keyPrefix);
    if (keys != null) {
      for (byte[] key : keys) {
        if (key[0] == '@') {
          Map<byte[], byte[]> map = JedisHelper.hgetAll(pool, key);
          for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
            byte[] mkey = JedisCacheHelper.buildCacheKeyFromKeyField(entry.getKey(), entry.getValue());

            if (logger.isDebugEnabled()) logger.debug("purge {}", new String(mkey));
            JedisHelper.del(pool, mkey);
          }
        }
      }

      if (logger.isDebugEnabled()) logger.debug("purge {}", keySet.get(keyPrefix));
      JedisHelper.del(pool, keys);
      return true;
    }

    return false;
  }
}
