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
  public static class UserPerm {
    private String entity;
    private long   permId;

    public static UserPerm fromString(String perm) {
      String part[] = perm.split(":");
      if (part.length == 1) {
        return new UserPerm(Long.parseLong(part[0]));
      } else if (part.length == 2) {
        return new UserPerm(part[1], Long.parseLong(part[0]));
      }
      return null;
    }

    public UserPerm() {
      this(null, Long.MAX_VALUE);
    }

    public UserPerm(long permId) {
      this(null, permId);
    }

    public UserPerm(String entity, long permId) {
      this.entity = entity;
      this.permId = permId;
    }

    public void setEntity(String entity) {
      this.entity = entity;
    }
    public String getEntity() {
      return entity;
    }

    public void setPermId(long permId) {
      this.permId = permId;
    }
    public long getPermId() {
      return permId;
    }

    private boolean entityEqual(String entity) {
      if (this.entity == null) {
        return entity == null;
      } else {
        return this.entity.equals(entity);
      }
    }

    public boolean canGrantPerm(long permId, String entity) {
      boolean idOk;
      if (this.permId <= 101) {  // Account.OWNER 101
        idOk = this.permId < permId;
      } else if (this.permId < 9_999) { // Account.PERM_EXIST 9_999
        idOk = this.permId <= permId;
      } else {
        idOk = this.permId == permId;
      }

      boolean entityOk = entityEqual(entity);
      return idOk && entityOk;   // TODO
    }

    public boolean canRevokePerm(long permId, String entity) {
      boolean idOk = true;
      if (this.permId < 9_999) {  // Account.PERM_EXIST 9_999
        idOk = this.permId < permId;
      }

      return idOk && entityEqual(entity);
    }

    public String toString() {
      return entity == null ? String.valueOf(permId) : String.valueOf(permId) + ":" + entity;
    }
  }
  public static class User {
    private Optional<Long>   uid;
    private Optional<String> openId;
    private String           name;
    private int              incId;
    private List<UserPerm>   perms;
    private boolean          internal = false;
    private boolean          anonymous = false;

    public static User internal() {
      User user = new User(0, "__", 0, Arrays.asList(new UserPerm(1L)));
      user.internal = true;
      return user;
    }

    public static User anonymous() {
      User user = new User(-1, "anonymous", -1, null);
      user.anonymous = true;
      return user;
    }

    public User(long uid, String name) {
      this(uid, name, Integer.MIN_VALUE, null);
    }

    public User(long uid, String name, int incId, List<UserPerm> perms) {
      this(uid, null, name, incId, perms);
    }

    public User(String openId, String name) {
      this(openId, name, Integer.MIN_VALUE, null);
    }

    public User(String openId, String name, int incId, List<UserPerm> perms) {
      this(-1, openId, name, incId, perms);
    }

    public User(long uid, String openId, String name) {
      this(uid, openId, name, Integer.MIN_VALUE, null);
    }

    public User(long uid, String openId, String name, int incId, List<UserPerm> perms) {
      if (uid > 0) this.uid = Optional.of(uid);
      if (openId != null) this.openId   = Optional.of(openId);

      this.name     = name;
      this.incId    = incId;
      this.perms  = perms;
      this.internal = false;
    }

    public boolean isOpen() {
      return !(uid != null && uid.isPresent());
    }

    public long getUid() {
      return uid.get();
    }

    public String getOpenId() {
      return openId == null ? null : openId.orElse(null);
    }
    public void setOpenId(String openId) {
      this.openId = Optional.of(openId);
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
      if (perms == null || perms.isEmpty()) return Long.MAX_VALUE;
      else return perms.get(0).getPermId();
    }

    public List<UserPerm> getPerms() {
      return perms == null ? Collections.emptyList() : perms;
    }

    public boolean hasPerm(String entity) {
      if (perms == null) return false;
      for (UserPerm perm : perms) {
        if (entity.equals(perm.getEntity())) return true;
      }
      return false;
    }

    public boolean canGrantPerm(long permId, String entity) {
      if (perms == null) return false;
      for (UserPerm perm : perms) {
        if (perm.canGrantPerm(permId, entity)) return true;
      }
      return false;
    }

    public boolean canRevokePerm(long permId, String entity) {
      if (perms == null) return false;
      for (UserPerm perm : perms) {
        if (perm.canRevokePerm(permId, entity)) return true;
      }
      return false;
    }

    public List<String> getEntitys() {
      List<String> entitys = new ArrayList<>();
      for (UserPerm perm : perms) {
        if (perm.getEntity() != null) entitys.add(perm.getEntity());
      }
      return entitys;
    }

    public List<Integer> getEntitysAsInt() {
      List<Integer> entitys = new ArrayList<>();
      for (UserPerm perm : perms) {
        if (perm.getEntity() != null) entitys.add(Integer.parseInt(perm.getEntity()));
      }
      return entitys;
    }

    public String getPermsString() {
      if (perms == null) return "";

      int i = 0;
      StringBuilder builder = new StringBuilder();
      for (UserPerm perm : perms) {
        if (i != 0) builder.append(",");
        builder.append(perm.toString());
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
      if (name == null) name = "";
      if (name.indexOf(':') != -1) name = name.replace(':', '_');
      return String.join(":", uid, token, name, createAt, incId, perms);
    }

    public User toUser() {
      int userIncId = Integer.MIN_VALUE;
      List<UserPerm> userPerms = null;

      if (!incId.isEmpty()) userIncId = Integer.parseInt(incId);
      if (!perms.isEmpty()) {
        userPerms = new ArrayList<>();
        for (String part : perms.split(",")) {
          UserPerm userPerm = UserPerm.fromString(part);
          if (userPerm != null) userPerms.add(userPerm);
        }
      }

      if (uid.indexOf('_') != -1) {
        return new User(uid, name, userIncId, userPerms);
      } else {
        return new User(Long.parseLong(uid), name, userIncId, userPerms);
      }
    }
  }

  private Set<String> tokenPool = new HashSet<>();
  private boolean     tokenInnerOnly = false;
  private Set<String> anonymousPool = new HashSet<>();
  private JedisPool   jedisPool;
  private String      domain;
  private int         maxAge;
  private List<String> excludeDomains;
  private String       cookiePrefix = "";

  private static final String KEY_PREFIX   = "RedisRMS_";

  private static final User internalUser = User.internal();
  private static List<GrantedAuthority> internalGrantedAuths = Arrays.asList(
    new SimpleGrantedAuthority("ROLE_SYSINTERNAL")
    );

  private static final User anonymousUser = User.anonymous();
  private static List<GrantedAuthority> anonymousGrantedAuths = Arrays.asList(
    new SimpleGrantedAuthority("ROLE_ANONYMOUS")
    );

  // login provider
  public RedisRememberMeService(JedisPool jedisPool, String domain, int maxAge) {
    this(jedisPool, "", true, domain, "", maxAge);
  }

  public RedisRememberMeService(JedisPool jedisPool, String tokenPool, boolean tokenInnerOnly,
                                String domain, String excludeDomain, int maxAge) {
    this(jedisPool, tokenPool, tokenInnerOnly, "", domain, excludeDomain, maxAge);
  }

  // login user
  public RedisRememberMeService(JedisPool jedisPool, String tokenPool, boolean tokenInnerOnly) {
    this(jedisPool, tokenPool, tokenInnerOnly, "");
  }

  public RedisRememberMeService(
    JedisPool jedisPool, String tokenPool, boolean tokenInnerOnly, String anonymousPool) {
    this(jedisPool, tokenPool, tokenInnerOnly, anonymousPool, "", "", 86400 * 7);
  }

  public RedisRememberMeService(
    JedisPool jedisPool, String tokenPool, boolean tokenInnerOnly,
    String anonymousPool, String domain, String excludeDomain, int maxAge) {

    this.jedisPool = jedisPool;

    this.domain = domain;
    this.maxAge = maxAge;

    this.tokenInnerOnly = tokenInnerOnly;

    if (tokenPool != null && !tokenPool.isEmpty()) {
      for (String token : tokenPool.split(",")) {
        this.tokenPool.add(token);
      }
    }

    if (anonymousPool != null && !anonymousPool.isEmpty()) {
      for (String token : anonymousPool.split(",")) {
        this.anonymousPool.add(token);
      }
    }

    if (excludeDomain.isEmpty()) {
      this.excludeDomains = new ArrayList<>();
    } else {
      this.excludeDomains = Arrays.asList(excludeDomain.split(","));
    }
  }

  public void setCookiePrefix(String cookiePrefix) {
    if (cookiePrefix != null && !cookiePrefix.isEmpty()) {
      this.cookiePrefix = cookiePrefix;
    }
  }

  private String cookieKey(String key) {
    return cookiePrefix.isEmpty() ? key : cookiePrefix + "_" + key;
  }

  private Cookie newCookie(String key, String value, int maxAge, boolean httpOnly) {
    return newCookie(key, value, maxAge, httpOnly, domain);
  }
  // the cookie's expire is controlled by server, not cookie's expire attribute
  private Cookie newCookie(String key, String value, int maxAge, boolean httpOnly, String domain) {
    Cookie cookie = new Cookie(cookieKey(key), value);
    cookie.setPath("/");
    cookie.setDomain(domain);

    // if maxAge > 0 ? cookie will be expired after 10 years.
    cookie.setMaxAge(maxAge > 0 ? 86400 * 3650 : maxAge);

    if (httpOnly) cookie.setHttpOnly(true);
    return cookie;
  }

  private String cacheKey(long uid) {
    return KEY_PREFIX + String.valueOf(uid);
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
      // clear cookie must before set cookie
      for (String excludeDomain : excludeDomains) {
        logoutImpl(response, excludeDomain);
      }

      response.addCookie(newCookie("uid", cacheEntity.uid, maxAge, false));
      if (user.getOpenId() != null) {
        response.addCookie(newCookie("openId", user.getOpenId(), maxAge, false));
      }
      response.addCookie(newCookie("token", token, maxAge, true));
    }

    return token;
  }

  private void logoutImpl(HttpServletResponse response) {
    logoutImpl(response, domain);
  }

  private void logoutImpl(HttpServletResponse response, String domain) {
    response.addCookie(newCookie("uid", null, 0, false, domain));
    response.addCookie(newCookie("openId", null, 0, false, domain));
    response.addCookie(newCookie("token", null, 0, true, domain));
  }

  public void logout(String id, HttpServletResponse response, boolean all) {
    if (all) {
      try (Jedis c = jedisPool.getResource()) {
        c.del(cacheKey(id));
      }
    }

    if (response != null) {
      logoutImpl(response);
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

    if (tokenInnerOnly && !"0".equals(request.getHeader("x-req-source"))) return null;
    return new RememberMeAuthenticationToken("N/A", internalUser, internalGrantedAuths);
  }

  Authentication autoLoginByAnonymous(HttpServletRequest request) {
    String anonymous = request.getHeader("x-req-anonymous");
    if (anonymous == null || !anonymousPool.contains(anonymous)) return null;

    return new RememberMeAuthenticationToken("N/A", anonymousUser, anonymousGrantedAuths);
  }

  Authentication autoLoginByRedisPool(HttpServletRequest request) {
    String token = null;
    String openId = null;

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(cookieKey("token"))) {
          token = cookie.getValue();
        } else if (cookie.getName().equals(cookieKey("openId"))) {
          openId = cookie.getValue();
        }
      }
    }

    if (token == null) {
      token = request.getHeader("authorization");
    }

    User user = checkToken(token);
    if (user == null) return null;

    if (openId != null) user.setOpenId(openId);

    List<GrantedAuthority> grantedAuths = new ArrayList<>();
    if (user.getPerms() != null) {
      for (UserPerm perm : user.getPerms()) {
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_PERM" + perm.toString()));
      }
    }

    return new RememberMeAuthenticationToken("N/A", user, grantedAuths);
  }

  @Override
  public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = autoLoginByTokenPool(request);
    if (auth != null) return auth;
    auth = autoLoginByAnonymous(request);
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
