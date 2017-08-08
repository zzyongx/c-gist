package example.manager;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;
import commons.utils.*;

@Component
public class MemoManager {
  @Autowired JedisPool jedisPool;
  public ApiResult list(String uid) {
    try (Jedis c = jedisPool.getResource()) {
      return new ApiResult<Map>(c.hgetAll(uid));
    }
  }

  public ApiResult add(String uid, String memo) {
    try (Jedis c = jedisPool.getResource()) {
      String uuid = UUID.randomUUID().toString();
      c.hset(uid, uuid, memo);
      return new ApiResult<Map>(MapHelper.make(uuid, memo));
    }
  }

  public ApiResult update(String uid, String memoId, String memo) {
    try (Jedis c = jedisPool.getResource()) {
      c.hset(uid, memoId, memo);
      return new ApiResult<Map>(MapHelper.make(memoId, memo));
    }
  }

  public ApiResult delete(String uid, String memoId) {
    try (Jedis c = jedisPool.getResource()) {
      c.hdel(uid, memoId);
    }
    return ApiResult.ok();
  }
}
