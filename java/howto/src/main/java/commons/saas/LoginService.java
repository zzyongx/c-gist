package commons.saas;

import java.util.Map;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import commons.utils.JsonHelper;

public abstract class LoginService {
  public static class User {
    private String openId;
    private String name;
    private String headImg;
    private Map<String, String> info;
    
    public void setOpenId(String openId) {
      this.openId = openId;
    }
    public String getOpenId() {
      return this.openId;
    }

    public void setName(String name) {
      this.name = name;
    }
    public String getName() {
      return this.name;
    }

    public void setHeadImg(String headImg) {
      this.headImg = headImg;
    }
    public String getHeadImg() {
      return this.headImg;
    }

    public void setInfo(Map<String, String> info) {
      this.info = info;
    }
    public Map<String, String> getInfo() {
      return this.info;
    }
  }
  
  private JedisPool jedisPool;
  
  public LoginService(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }
    
  protected abstract User doLogin(String tmpToken);

  public User login(String tmpToken) {
    User user = doLogin(tmpToken);
    if (user != null) {
      try (Jedis c = jedisPool.getResource()) {
        c.set(user.getOpenId(), JsonHelper.writeValueAsString(user));
      }
    }
    return user;
  }
  
  public User info(String openId) {
    User user = null;
    try (Jedis c = jedisPool.getResource()) {
      String s = c.get(openId);
      if (s != null) {
        user = JsonHelper.readValue(s, User.class);
      }
    }
    return user;
  }
  
  public void logout(String openId) {
    try (Jedis c = jedisPool.getResource()) {
      c.del(openId);
    }
  }
}
