package lib;

@Grab('redis.clients:jedis:2.8.1')
import redis.clients.jedis.Jedis;

class EnvScaffold {
  static jedis = [host: "127.0.0.1", port: 6379, db: 0]

  static logout(uid) {
    new Jedis(jedis.host, jedis.port).del("RedisRMS_" + uid);
  }

  static login(Map param) {
    def uid = param.get("uid")
    def token = param.get("token")
    def name = param.get("name") ?: "test"
    def createAt = String.valueOf(System.currentTimeMillis().intdiv(1000));
    def incId = param.get("incId") ?: ""
    def perms = param.get("perms") ?: ""

    def tmpJedis = param.get("jedis")
    if (tmpJedis) jedis.putAll(tmpJedis);

    def value = String.join(":", uid, token, name, createAt, incId, perms)
    def c = new Jedis(jedis.host, jedis.port);
    c.select(jedis.db)
    c.setex("RedisRMS_" + uid, 3600, value)
  }
}
