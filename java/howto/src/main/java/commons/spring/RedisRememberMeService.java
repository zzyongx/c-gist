package commons.spring;

import java.io.*;
import java.util.*;
import javax.sql.DataSource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.web.authentication.RememberMeServices;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import commons.utils.AesHelper;
import commons.utils.HttpHelper;
import commons.utils.StringHelper;
import commons.utils.Tuple2;

public class RedisRememberMeService implements RememberMeServices {
  public static class UserPerm {
    private String entity;
    private long   permId;
    private int    flag = 0;

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
      this(entity, permId, 0);
    }

    public UserPerm(String entity, long permId, int flag) {
      this.entity = entity;
      this.permId = permId;
      this.flag = flag;
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

    public void setFlag(int flag) {
      this.flag = flag;
    }
    public int getFlag() {
      return this.flag;
    }

    public boolean entityEqual(String entity) {
      return entityEqual(entity, false);
    }

    public boolean entityEqual(String entity, boolean xpath) {
      if (this.entity == null) {
        return entity == null;
      } else if (entity == null || (!xpath && entity.indexOf('-') == -1)) {
        return this.entity.equals(entity);
      } else {
        String a[] = this.entity.split("-");
        String b[] = entity.split("-");

        // if !xpath A-B has perm A-B-C
        // if xpath A-B-C has perm A-B, A-B has perm A-B-C

        int i;
        for (i = 0; i < a.length && i < b.length; ++i) {
          if (!a[i].equals(b[i])) return false;
        }
        if (xpath) return true;
        else return i == a.length && i <= b.length;
      }
    }

    public boolean permEqual(long permId, String entity) {
      if (this.permId != permId) return false;

      if (this.entity == null) return entity == null;
      else return this.entity.equals(entity);
    }

    public boolean canFactGrantPerm(long permId, String entity) {
      return canGrantPerm(permId, entity, false, true);
    }

    public boolean canGrantPerm(long permId, String entity) {
      return canGrantPerm(permId, entity, false, false);
    }

    private Optional<Boolean> checkPermException(long permId, List<Tuple2<Boolean, Long>> permIds) {
      if (permIds != null) {
        for (Tuple2<Boolean, Long> tuple : permIds) {
          if (tuple.s == permId) return Optional.of(tuple.f);
        }
      }
      return Optional.empty();
    }

    public Optional<Boolean> checkPermException(long permId) {
      Optional<Boolean> r = checkPermException(permId, configPermException.get(-1L));
      return r.isPresent() ? r : checkPermException(permId, configPermException.get(this.permId));
    }

    public boolean canGrantPerm(long permId, String entity, boolean xpath) {
      return canGrantPerm(permId, entity, xpath, false);
    }

    public boolean canGrantPerm(long permId, String entity, boolean xpath, boolean fact) {
      boolean idOk;
      if (this.permId < 100) {          // Account.BOSS 100
        return false;
      } else if (this.permId <= 101) {  // Account.OWNER 101
        idOk = this.permId < permId;
      } else if (fact && configGrantPermId > 0) {
        idOk = (this.permId <= configGrantPermId && this.permId <= permId);
      } else if (this.permId < 9_999) { // Account.PERM_EXIST 9_999
        idOk = checkPermException(permId).orElse(this.permId <= permId);
      } else {
        idOk = checkPermException(permId).orElse(this.permId == permId);
      }

      return idOk && entityEqual(entity, xpath);
    }

    public boolean canRevokePerm(long permId, String entity) {
      boolean idOk = true;
      if (this.permId < 100) {           // Account.BOSS 100
        return false;
      } else if (configGrantPermId > 0) {
        idOk = (this.permId <= configGrantPermId && this.permId < permId);
      } else if (this.permId < 9_999) {  // Account.PERM_EXIST 9_999
        idOk = checkPermException(permId).orElse(
          configRevokePermStrict ? this.permId < permId : this.permId <= permId);
      } else {
        idOk = checkPermException(permId).orElse(this.permId == permId);
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
    private boolean          internal  = false;
    private boolean          api       = false;
    private boolean          anonymous = false;
    private User             origin    = null;

    public static User internal() {
      User user = new User(0, "__", 0, Arrays.asList(new UserPerm(1L)));
      user.internal = true;
      return user;
    }

    // api user run as admin
    public static User apiUser(String entity) {
      User user = new User(1, "__", -1, Arrays.asList(new UserPerm(entity, 201)));
      user.api = true;
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
      if (uid >= 0) this.uid = Optional.of(uid);
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

    public boolean isApi() {
      return api;
    }

    public boolean isPlatformBoss() {
      return getPerm() == 0;
    }

    public boolean isPlatformAdmin() {
      long permId = getPerm();
      return permId <= 10 && permId > 0;
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

    public boolean canFactGrantPerm(long permId, String entity) {
      return canGrantPerm(permId, entity, false, true);
    }

    public boolean canGrantPerm(long permId, String entity) {
      return canGrantPerm(permId, entity, false, false);
    }

    public boolean canGrantPerm(long permId, String entity, boolean xpath) {
      return canGrantPerm(permId, entity, xpath, false);
    }

    public boolean canGrantPerm(long permId, String entity, boolean xpath, boolean fact) {
      if (isInternal()) return true;

      if (perms == null) return false;
      for (UserPerm perm : perms) {
        if (perm.canGrantPerm(permId, entity, xpath, fact)) return true;
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

    public boolean canSu(long permId) {
      if (perms == null) return false;
      for (UserPerm perm : perms) {
        long myPermId = perm.getPermId();
        if (myPermId > 0 && myPermId < 100 && myPermId <= permId) return true;
      }
      return false;
    }

    public List<String> getEntitys() {
      List<String> entitys = new ArrayList<>();
      if (perms == null) return entitys;
      for (UserPerm perm : perms) {
        if (perm.getEntity() != null) entitys.add(perm.getEntity());
      }
      return entitys;
    }

    public List<Integer> getEntitysAsInt() {
      List<Integer> entitys = new ArrayList<>();
      if (perms == null) return entitys;
      for (UserPerm perm : perms) {
        try {
          if (perm.getEntity() != null) {
            entitys.add(Integer.parseInt(perm.getEntity()));
          }
        } catch (NumberFormatException e) {}
      }
      return entitys;
    }

    public void setOrigin(User origin) {
      this.origin = origin;
    }
    public User getOrigin() {
      return this.origin;
    }

    public void removePerm() {
      this.incId = -1;
      this.perms = null;
    }

    public String getPermsString() {
      if (perms == null) return "";

      int i = 0;
      StringBuilder builder = new StringBuilder();
      for (UserPerm perm : perms) {
        if (perm.getPermId() < 0) continue;

        if (i != 0) builder.append(",");
        builder.append(perm.toString());
        i++;
      }
      return builder.toString();
    }

    public String toString() {
      return getId() + ":" + getPermsString();
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
      return toUser(null);
    }

    public User toUser(User origin) {
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

      User user;
      if (uid.indexOf('_') != -1) {
        user = new User(uid, name, userIncId, userPerms);
      } else {
        user = new User(Long.parseLong(uid), name, userIncId, userPerms);
      }

      user.setOrigin(origin);
      return user;
    }
  }

  private Set<String> tokenPool = new HashSet<>();
  private boolean     tokenInnerOnly = false;
  private Set<String> anonymousPool = new HashSet<>();
  private JedisPool   jedisPool;
  private int         maxAge;
  private List<String> domains;
  private List<String> excludeDomains;
  private String       cookiePrefix = "";

  private DataSource dataSource;
  private String     apiAuthSql;

  private AesHelper  aesHelper = null;

  private static final String KEY_PREFIX    = "RedisRMS_";
  private static final String ENCKEY_PREFIX = "RedisEncRMS_";
  private static int configGrantPermId = -1;
  private static boolean configRevokePermStrict = true;
  private static Map<Long, List<Tuple2<Boolean, Long>>> configPermException = new HashMap<>();

  private static int configSuPermId = -1;

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

    if (domain == null || domain.isEmpty()) {
      this.domains = Arrays.asList("");
    } else {
      this.domains = Arrays.asList(domain.split(","));
    }

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

  /* SELECT f2 AS pass FROM tbl WHERE user = ? */
  public void setApiAuthDb(DataSource dataSource, String sql) {
    this.dataSource = dataSource;
    this.apiAuthSql = sql;
  }

  public void setCookiePrefix(String cookiePrefix) {
    if (cookiePrefix != null && !cookiePrefix.isEmpty()) {
      this.cookiePrefix = cookiePrefix;
    }
  }

  public void setGrantPermId(int grantPermId) {
    this.configGrantPermId = grantPermId;
  }

  public void setRevokePermStrict(boolean strict) {
    this.configRevokePermStrict = strict;
  }

  public void setSuPermId(int suPermId) {
    this.configSuPermId = suPermId;
  }

  public void setPermException(String permException) {
    if (permException != null && !permException.isEmpty()) {
      for (String o : permException.split(";")) {
        String[] kv = o.split(":", 2);
        if (kv.length != 2) continue;

        long permId = "*".equals(kv[0]) ? -1L : Long.parseLong(kv[0]);

        for (String permStr : kv[1].split(",")) {
          Tuple2<Boolean, Long> perm;
          if (permStr.charAt(0) == '!') {
            perm = new Tuple2<>(false, Long.parseLong(permStr.substring(1)));
          } else {
            perm = new Tuple2<>(true, Long.parseLong(permStr));
          }
          this.configPermException.computeIfAbsent(permId, k -> new ArrayList<>()).add(perm);
        }
      }
    }
  }

  public void setAesKey(String keyFile) {
    if (keyFile != null && !keyFile.isEmpty()) {
      aesHelper = new AesHelper(keyFile);
    }
  }

  private String cookieKey(String key) {
    return cookiePrefix.isEmpty() ? key : cookiePrefix + "_" + key;
  }

  public void addCookies(HttpServletResponse response, String key, String value, int maxAge, boolean httpOnly) {
    addCookies(response, key, value, maxAge, httpOnly, domains);
  }
  // the cookie's expire is controlled by server, not cookie's expire attribute
  private void addCookies(HttpServletResponse response, String key, String value, int maxAge, boolean httpOnly,
                          List<String> domains) {
    for (String domain : domains) {
      Cookie cookie = new Cookie(cookieKey(key), value);
      cookie.setPath("/");

      // Specifies those hosts to which the cookie will be sent. If not specified, defaults to the host portion of the current document location (but not including subdomains).
      if (!domain.isEmpty()) cookie.setDomain(domain);

      // if maxAge > 0 ? cookie will be expired after 10 years.
      cookie.setMaxAge(maxAge > 0 ? 86400 * 3650 : maxAge);

      if (httpOnly) cookie.setHttpOnly(true);
      response.addCookie(cookie);
    }
  }

  private String cacheKey(long uid) {
    return cacheKey(String.valueOf(uid));
  }

  private String cacheKey(String uid) {
    if (aesHelper == null) return KEY_PREFIX + uid;
    else return ENCKEY_PREFIX + uid;
  }

  private String cacheValue(String value) {
    if (aesHelper == null) return value;
    else return aesHelper.encrypt(value);
  }

  private String valueFromCache(String cache) {
    if (cache == null || cache.isEmpty()) return cache;

    if (aesHelper == null) return cache;
    else return aesHelper.decrypt(cache);
  }

  private String cacheApiKey(String uid) {
    return KEY_PREFIX + "API_" + uid;
  }

  public String login(HttpServletResponse response, User user) {
    return login(response, user, false);
  }

  public String login(HttpServletResponse response, User user, boolean relogin) {
    CacheEntity cacheEntity = CacheEntity.buildFromUser(user);

    try (Jedis c = jedisPool.getResource()) {
      String key = cacheKey(cacheEntity.uid);
      String token = valueFromCache(c.get(key));
      if (token != null && !relogin) {
        String parts[] = token.split(":");
        if (parts.length >= 2) {
          cacheEntity.token = parts[1];
        }
      }
      c.setex(KEY_PREFIX + cacheEntity.uid, maxAge, cacheEntity.toString());  // TODO: delete
      c.setex(cacheKey(cacheEntity.uid), maxAge, cacheValue(cacheEntity.toString()));

    }

    String token = cacheEntity.getCookieToken();

    if (response != null) {
      // clear cookie must before set cookie
      logoutImpl(response, excludeDomains);

      addCookies(response, "uid", cacheEntity.uid, maxAge, false);
      if (user.getOpenId() != null) {
        addCookies(response, "openId", user.getOpenId(), maxAge, false);
      } else {
        addCookies(response, "openId", null, 0, false);
      }
      addCookies(response, "token", token, maxAge, true);
    }

    return token;
  }

  private void logoutImpl(HttpServletResponse response) {
    logoutImpl(response, domains);
  }

  private void logoutImpl(HttpServletResponse response, List<String> domains) {
    addCookies(response, "uid", null, 0, false, domains);
    addCookies(response, "openId", null, 0, false, domains);
    addCookies(response, "token", null, 0, true, domains);
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
      cacheEntity = CacheEntity.buildFromString(valueFromCache(c.get(key)));
      if (cacheEntity == null) return false;

      cacheEntity.name  = user.getName();
      cacheEntity.incId = user.getIncIdString();
      cacheEntity.perms = user.getPermsString();
      c.setex(KEY_PREFIX + user.getId(), maxAge, cacheEntity.toString());  // TODO: delete
      c.setex(key, maxAge, cacheValue(cacheEntity.toString()));
    }

    return true;
  }

  private User suUser(User user, String suId) {
    if (suId != null && !suId.isEmpty() && user != null) {
      if (configSuPermId > 0 && user.canSu(configSuPermId)) {
        try (Jedis c = jedisPool.getResource()) {
          CacheEntity cacheEntity = CacheEntity.buildFromString(valueFromCache(c.get(cacheKey(suId))));
          return cacheEntity == null ? null : cacheEntity.toUser(user);
        }
      } else {
        return null;
      }
    }
    return user;
  }

  private User checkToken(String token, String suId) {
    if (token == null) return null;

    String parts[] = token.split(":", 2);
    if (parts.length != 2) return null;

    String key = cacheKey(parts[0]);

    CacheEntity cacheEntity = null;
    try (Jedis c = jedisPool.getResource()) {
      cacheEntity = CacheEntity.buildFromString(valueFromCache(c.get(key)));
    }

    if (cacheEntity == null) return null;

    if (!cacheEntity.uid.equals(parts[0]) || !cacheEntity.token.equals(parts[1])) {
      return null;
    }

    if (cacheEntity.beforeExpire(maxAge)) {
      try (Jedis c = jedisPool.getResource()) {
        c.setex(KEY_PREFIX + parts[0], maxAge, cacheEntity.toString());  // TODO: delete
        c.setex(key, maxAge, cacheValue(cacheEntity.toString()));
      }
    }

    return suUser(cacheEntity.toUser(), suId);
  }

  private Authentication autoLoginByTokenPool(HttpServletRequest request) {
    if (tokenPool.isEmpty()) return null;

    String queryString = request.getParameter("__token");
    if (queryString == null) {
      queryString = request.getHeader("authorization");
    }
    if (queryString == null || !tokenPool.contains(queryString)) return null;

    if (tokenInnerOnly && !"0".equals(request.getHeader("x-req-source"))) return null;
    return new RememberMeAuthenticationToken("N/A", internalUser, internalGrantedAuths);
  }

  private Authentication autoLoginByAnonymous(HttpServletRequest request) {
    String anonymous = request.getHeader("x-req-anonymous");
    if (anonymous == null || !anonymousPool.contains(anonymous)) return null;

    return new RememberMeAuthenticationToken("N/A", anonymousUser, anonymousGrantedAuths);
  }

  private Authentication autoLoginByRedisPool(HttpServletRequest request) {
    String token = null;
    String openId = null;
    String suId = null;

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(cookieKey("token"))) {
          token = cookie.getValue();
        } else if (cookie.getName().equals(cookieKey("openId"))) {
          openId = cookie.getValue();
        } else if (cookie.getName().equals(cookieKey("suId"))) {
          suId = cookie.getValue();
        }
      }
    }

    if (token == null) token = request.getHeader("authorization");
    if (suId == null) suId = request.getParameter("__su");

    User user = checkToken(token, suId);
    if (user == null) return null;

    if (openId != null) user.setOpenId(openId);

    // su user only allow get
    if (user.getOrigin() != null && !request.getMethod().equals("GET")) {
      user.removePerm();
    }

    List<GrantedAuthority> grantedAuths = new ArrayList<>();
    if (user.getPerms() != null) {
      for (UserPerm perm : user.getPerms()) {
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_PERM" + perm.toString()));
      }
    }

    request.setAttribute("RmsUid", user.getId());
    return new RememberMeAuthenticationToken("N/A", user, grantedAuths);
  }

  public static class AuthDbRow {
    private String pass;
    private List<String> cidrs;

    public static AuthDbRow parse(String s) {
      String part[] = s.split(":", 2);
      if (part.length == 1) return new AuthDbRow(part[0]);
      else return new AuthDbRow(part[0], Arrays.asList(part[1].split(",")));
    }

    public AuthDbRow() {}

    public AuthDbRow(String pass) {
      this.pass = pass;
    }

    public AuthDbRow(String pass, List<String> cidrs) {
      this.pass = pass;
      this.cidrs = cidrs;
    }

    public void setPass(String pass) {
      this.pass = pass;
    }
    public String getPass() {
      return this.pass;
    }

    public void setCidrs(String cidrs) {
      if (cidrs != null && !cidrs.isEmpty()) {
        this.cidrs = Arrays.asList(cidrs.split(","));
      }
    }
    public List<String> getCidrs() {
      return this.cidrs;
    }

    public boolean match(String pass, String ip) {
      // disable api access
      if (this.pass == null || this.pass.isEmpty()) return false;

      if (!this.pass.equals(pass)) return false;

      if (cidrs == null || cidrs.isEmpty()) return true;
      for (String cidr : cidrs) {
        boolean mat;
        int idx = cidr.indexOf('/');
        if (idx > 0) {  // found and not the first
          mat = new SubnetUtils(cidr).getInfo().isInRange(ip);
        } else {
          mat = cidr.equals(ip);
        }
        if (mat) return true;
      }
      return false;
    }

    public String toString() {
      return (cidrs == null || cidrs.isEmpty()) ? pass : pass + ":" + String.join(",", cidrs);
    }
  }

  private Authentication autoLoginByApiAuthDb(HttpServletRequest request) {
    if (dataSource == null || apiAuthSql == null) return null;

    String appKey    = request.getHeader("x-auth-appkey");
    String appSecret = request.getHeader("x-auth-appsecret");
    if (appKey == null || appSecret == null) return null;

    AuthDbRow authDbRow = null;
    try (Jedis c = jedisPool.getResource()) {
      String value = c.get(cacheApiKey(appKey));
      if (value != null) authDbRow = AuthDbRow.parse(value);
    }

    if (authDbRow == null || !authDbRow.match(appSecret, HttpHelper.getClientIp(request))) {
      JdbcTemplate jdbc = new JdbcTemplate(dataSource);
      authDbRow = jdbc.queryForObject(apiAuthSql, new BeanPropertyRowMapper<AuthDbRow>(AuthDbRow.class), appKey);

      if (authDbRow == null || !authDbRow.match(appSecret, HttpHelper.getClientIp(request))) return null;

      try (Jedis c = jedisPool.getResource()) {
        c.setex(cacheApiKey(appKey), 300, authDbRow.toString());
      }
    }

    List<GrantedAuthority> grantedAuths = Arrays.asList(
      new SimpleGrantedAuthority("ROLE_PERM201"));

    request.setAttribute("RmsUid", "api-" + appKey);
    return new RememberMeAuthenticationToken("N/A", User.apiUser(appKey), grantedAuths);
  }

  @Override
  public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = autoLoginByTokenPool(request);
    if (auth != null) return auth;
    auth = autoLoginByAnonymous(request);
    if (auth != null) return auth;
    auth = autoLoginByRedisPool(request);
    if (auth != null) return auth;
    return autoLoginByApiAuthDb(request);
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
