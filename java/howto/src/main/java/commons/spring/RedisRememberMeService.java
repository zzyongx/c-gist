package commons.spring;

import java.io.*;
import java.util.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.web.authentication.RememberMeServices;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import commons.utils.StringHelper;

public class RedisRememberMeService implements RememberMeServices {
  public static class User {
    private Optional<Long>   uid;
    private Optional<String> openId;
    private String           name;
    private int              incId; 
    private List<Long>       permIds;
    private boolean          internal;

    public static User internal() {
      User user = new User(0, "__", 0, Arrays.asList(1L));      
      user.internal = true;
      return user;
    }      

    public User(long uid, String name) {
      this(uid, name, Integer.MIN_VALUE, null);
    }

    public User(long uid, String name, int incId, List<Long> permIds) {
      this.uid      = Optional.of(uid);
      this.name     = name;
      this.incId    = incId;
      this.permIds  = permIds;
      this.internal = false;
    }

    public User(String openId, String name) {
      this(openId, name, Integer.MIN_VALUE, null);
    }

    public User(String openId, String name, int incId, List<Long> permIds) {
      this.openId   = Optional.of(openId);
      this.name     = name;
      this.incId    = incId;
      this.permIds  = permIds;
      this.internal = false;
    }

    public boolean isOpen() {
      return !(uid != null && uid.isPresent());
    }

    public long getUid() {
      return uid.get();
    }

    public void setName(String name) {
      this.name = name;
    }
    public String getName() {
      return name;
    }

    public String getId() {
      if (uid != null && uid.isPresent()) return String.valueOf(uid.get());
      else if (openId != null && openId.isPresent()) return openId.get();
      else return null;
    }

    public int getIncId() {
      return this.incId;
    }

    public String getIncIdString() {
      return incId == Integer.MIN_VALUE ? "" : String.valueOf(incId);
    }

    public boolean isInternal() {
      return internal;
    }

    public boolean isPlatformBoss() {
      return getPerm() == 0;
    }

    public boolean isPlatformAdmin() {
      return getPerm() <= 10;
    }

    public boolean isBoss() {
      return getPerm() == 100;
    }

    public long getPerm() {
      if (permIds == null) return Long.MAX_VALUE;
      else return permIds.get(0);
    }

    public List<Long> getPerms() {
      return permIds;
    }
    
    public String getPermsString() {
      if (permIds == null) return "";

      int i = 0;
      StringBuilder builder = new StringBuilder();
      for (Long perm : permIds) {
        if (i != 0) builder.append(",");
        builder.append(String.valueOf(perm));
        i++;
      }
      return builder.toString();
    }
  }

  private static class CacheEntity {
    private static final int PARTS_NUMBER = 6;
    
    public String uid;
    public String token;
    public String name;
    public String createAt;
    public String incId;
    public String perms;

    public boolean beforeExpire(int maxAge) {
      long createAt = Long.parseLong(this.createAt);
      long now = System.currentTimeMillis()/1000;
      if (createAt + maxAge/2 < now) {
        this.createAt = Long.toString(now);
        return true;
      } else {
        return false;
      }
    }

    public static CacheEntity buildFromString(String token) {  
      if (token == null) return null;
      String parts[] = token.split(":", PARTS_NUMBER);
      if (parts.length != PARTS_NUMBER) return null;

      CacheEntity entity = new CacheEntity();
      entity.uid      = parts[0];
      entity.token    = parts[1];
      entity.name     = parts[2];
      entity.createAt = parts[3];
      entity.incId    = parts[4];
      entity.perms    = parts[5];

      return entity;
    }

    public static CacheEntity buildFromUser(User user) {
      CacheEntity entity = new CacheEntity();
      entity.uid      = user.getId();
      entity.token    = StringHelper.random(32);
      entity.name     = user.getName() == null ? "" : user.getName();
      entity.createAt = Long.toString(System.currentTimeMillis()/1000);
      entity.incId    = user.getIncIdString();
      entity.perms    = user.getPermsString();
      return entity;
    }

    public String getCookieToken() {
      return String.join(":", uid, token);
    }

    public String toString() {
      if (name.indexOf(':') != -1) name = name.replace(':', '_');
      return String.join(":", uid, token, name, createAt, incId, perms);
    }

    public User toUser() {
      int userIncId = Integer.MIN_VALUE;
      List<Long> permIds = null;

      if (!incId.isEmpty()) userIncId = Integer.parseInt(incId);
      if (!perms.isEmpty()) {
          permIds = new ArrayList<>();
          for (String part : perms.split(",")) {
            permIds.add(Long.parseLong(part));
          }
      }
      
      if (uid.indexOf('_') != -1) {
        return new User(uid, name, userIncId, permIds);
      } else {
        return new User(Long.parseLong(uid), name, userIncId, permIds);
      }
    }
  }

  private Set<String> tokenPool = new HashSet<>();
  private JedisPool jedisPool;
  private String    domain;
  private int       maxAge;
  private static final String KEY_PREFIX   = "RedisRMS_";
  private static final User internalUser = User.internal();
  private static List<GrantedAuthority> internalGrantedAuths = Arrays.asList(
    new SimpleGrantedAuthority("ROLE_SYSINTERNAL")
    );
  

  public RedisRememberMeService(JedisPool jedisPool, String domain, int maxAge) {
    this(jedisPool, "", domain, maxAge);
  }
  public RedisRememberMeService(JedisPool jedisPool, String tokenPool, String domain, int maxAge) {
    this.jedisPool = jedisPool;
    this.domain = domain;
    this.maxAge = maxAge;

    for (String token : tokenPool.split(",")) {
      this.tokenPool.add(token);
    }
  }

  private Cookie newCookie(String key, String value, int maxAge, boolean httpOnly) {
    Cookie cookie = new Cookie(key, value);
    cookie.setPath("/");
    cookie.setDomain(domain);
    cookie.setMaxAge(maxAge);
    if (httpOnly) cookie.setHttpOnly(true);
    return cookie;
  }    

  private String cacheKey(String uid) {
    return KEY_PREFIX + uid;
  }
  
  public String login(HttpServletResponse response, User user) {
    CacheEntity cacheEntity = CacheEntity.buildFromUser(user);

    try (Jedis c = jedisPool.getResource()) {
      String key = cacheKey(cacheEntity.uid);
      String token = c.get(key);
      if (token != null) {
        String parts[] = token.split(":");
        if (parts.length >= 2) {
          cacheEntity.token = parts[1];
        }
      }
      c.setex(cacheKey(cacheEntity.uid), maxAge, cacheEntity.toString());
    }

    String token = cacheEntity.getCookieToken();

    if (response != null) {
      response.addCookie(newCookie("uid", cacheEntity.uid, maxAge, false));
      response.addCookie(newCookie("token", token, maxAge, true));
    }

    return token;    
  }

  public void logout(String id, HttpServletResponse response) {
    try (Jedis c = jedisPool.getResource()) {
      c.del(cacheKey(id));
    }
    
    if (response != null) {
      response.addCookie(newCookie("uid", null, 0, false));
      response.addCookie(newCookie("token", null, 0, true));
    }
  }

  public boolean update(User user) {
    CacheEntity cacheEntity;
    
    try (Jedis c = jedisPool.getResource()) {
      String key = cacheKey(user.getId());
      cacheEntity = CacheEntity.buildFromString(c.get(key));
      if (cacheEntity == null) return false;
      
      cacheEntity.name  = user.getName();
      cacheEntity.incId = user.getIncIdString();
      cacheEntity.perms = user.getPermsString();
      c.setex(key, maxAge, cacheEntity.toString());
    }
    
    return true;
  }

  private User checkToken(String token) {
    if (token == null) return null;
    
    String parts[] = token.split(":", 2);
    if (parts.length != 2) return null;

    String key = cacheKey(parts[0]);

    CacheEntity cacheEntity = null;
    try (Jedis c = jedisPool.getResource()) {
      cacheEntity = CacheEntity.buildFromString(c.get(key));
    }
    
    if (cacheEntity == null) return null;
    
    if (!cacheEntity.uid.equals(parts[0]) || !cacheEntity.token.equals(parts[1])) {
      return null;
    }

    if (cacheEntity.beforeExpire(maxAge)) {
      try (Jedis c = jedisPool.getResource()) {
        c.setex(key, maxAge, cacheEntity.toString());
      }
    }

    return cacheEntity.toUser();
  }

  Authentication autoLoginByTokenPool(HttpServletRequest request) {
    if (tokenPool.isEmpty()) return null;
    
    String queryString = request.getParameter("__token");
    if (queryString == null) {
      queryString = request.getHeader("authorization");
    }
    if (queryString == null || !tokenPool.contains(queryString)) return null;

    return new RememberMeAuthenticationToken("N/A", internalUser, internalGrantedAuths);
  }

  Authentication autoLoginByRedisPool(HttpServletRequest request) {
    String token = null;
    
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("token")) {
          token = cookie.getValue();
          break;
        }
      }
    }

    if (token == null) {
      token = request.getHeader("authorization");
    }

    User user = checkToken(token);
    if (user == null) return null;

    List<GrantedAuthority> grantedAuths = new ArrayList<>();
    if (user.getPerms() != null) {
      for (Long perm : user.getPerms()) {
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_PERM" + String.valueOf(perm)));
      }
    }

    return new RememberMeAuthenticationToken("N/A", user, grantedAuths);
  }

  @Override
  public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = autoLoginByTokenPool(request);
    if (auth != null) return auth;
    return autoLoginByRedisPool(request);
  }

  @Override
  public void loginFail(HttpServletRequest request, HttpServletResponse response) {
    System.out.println("loginFaile");
  }

  @Override
  public void loginSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication auth) {
    System.out.println("loginSuccess");
  }
}
