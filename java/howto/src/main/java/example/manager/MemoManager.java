package example.manager;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;
import example.model.*;

@Component
public class MemoManager {
  @Autowired JedisPool jedisPool;

  final String KEY = "memo";

  public ApiResult list() {
    try (Jedis c = jedisPool.getResource()) {
      return new ApiResult<Map>(c.hgetAll(KEY));
    }
  }

  public ApiResult add(String memo) {
    try (Jedis c = jedisPool.getResource()) {
      String uuid = UUID.randomUUID().toString();
      c.hset(KEY, uuid, memo);
      return new ApiResult<Map>(new HashMap<String, String>() {{
        put(uuid, memo);
      }});
    }
  }

  public ApiResult update(String memoId, String memo) {
    try (Jedis c = jedisPool.getResource()) {
      c.hset(KEY, memoId, memo);
      return new ApiResult<Map>(new HashMap<String, String>() {{
        put(memoId, memo);
      }});
    }
  }

  public ApiResult delete(String memoId) {
    try (Jedis c = jedisPool.getResource()) {
      c.hdel(KEY, memoId);
    }
    return ApiResult.ok();
  }
}
