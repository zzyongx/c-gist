package commons.saas;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import commons.utils.PatchcaHelper;

public class PatchcaService {
  private final static String PATCHCA_PREFIX = "PatchcaService_";
  private JedisPool jedisPool;
  
  public PatchcaService(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }
  
  public byte[] getPatchca(String id) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      String text = PatchcaHelper.get(os);
      os.flush();

      try (Jedis c = jedisPool.getResource()) {
        c.setex(PATCHCA_PREFIX + id, 60, text);
      }

      return os.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getText(String id) {
    try (Jedis c = jedisPool.getResource()) {
      return c.get(PATCHCA_PREFIX + id);
    }
  }

  public boolean checkText(String id, String value) {
    try (Jedis c = jedisPool.getResource()) {
      String key = PATCHCA_PREFIX + id;
      boolean r = value.equalsIgnoreCase(c.get(key));
      c.del(key);
      return r;
    }
  }
}
