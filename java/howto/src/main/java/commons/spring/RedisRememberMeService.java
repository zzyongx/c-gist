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
    private long uid;
    private int  perm;

    public User(long uid) {
      this(uid, Integer.MIN_VALUE);
    }

    public User(long uid, int perm) {
      this.uid  = uid;
      this.perm = perm;
    }

    public long getUid() {
      return this.uid;
    }

    public int getPerm() {
      return this.perm;
    }    
  }
  
  private JedisPool jedisPool;
  private String    domain;
  private int       maxAge;
  private String    KEY_PREFIX = "RedisRMS_";

  public RedisRememberMeService(JedisPool jedisPool, String domain, int maxAge) {
    this.jedisPool = jedisPool;
    this.domain = domain;
    this.maxAge = maxAge;
  }

  public String login(HttpServletResponse response, User user) {
    String uid  = String.valueOf(user.getUid());
    String perm = user.getPerm() >= 0 ? String.valueOf(user.getPerm()) : "";

    String token = String.join(":", uid, StringHelper.random(32));

    try (Jedis c = jedisPool.getResource()) {
      c.setex(KEY_PREFIX + uid, maxAge,
              String.join(":", token, perm, Long.toString(System.currentTimeMillis()/1000)));
    }

    if (response != null) {
      Cookie uidCookie = new Cookie("uid", uid);
      uidCookie.setPath("/");
      uidCookie.setDomain(domain);
      uidCookie.setMaxAge(maxAge);
      response.addCookie(uidCookie);

      Cookie tokenCookie = new Cookie("token", token);
      tokenCookie.setPath("/");
      tokenCookie.setDomain(domain);
      tokenCookie.setMaxAge(maxAge);
      tokenCookie.setHttpOnly(true);
      response.addCookie(tokenCookie);
    }

    return token;    
  }

  public void logout(long uid, HttpServletResponse response) {
    try (Jedis c = jedisPool.getResource()) {
      c.del(KEY_PREFIX + String.valueOf(uid));
    }
    
    if (response != null) {
      Cookie uidCookie = new Cookie("uid", null);
      uidCookie.setMaxAge(0);
      uidCookie.setPath("/");
      uidCookie.setDomain(domain);
      response.addCookie(uidCookie);

      Cookie token = new Cookie("token", null);
      token.setMaxAge(0);
      token.setPath("/");
      token.setDomain(domain);
      response.addCookie(token);
    }
  }

  public boolean updatePerm(long uid, int perm) {
    String token = null;
    try (Jedis c = jedisPool.getResource()) {
      String key = KEY_PREFIX + String.valueOf(uid);
      token = c.get(key);
      
      if (token == null) return false;
      String parts[] = token.split(":", 4);
      if (parts.length != 4) return false;
      
      c.setex(key, maxAge,
              String.join(":", parts[0], parts[1], String.valueOf(perm), parts[3]));
    }
    return true;
  }

  private User checkToken(String token) {
    if (token == null) return null;
    
    String parts[] = token.split(":", 2);
    if (parts.length != 2) return null;

    String savedToken = null;
    try (Jedis c = jedisPool.getResource()) {
      savedToken = c.get(KEY_PREFIX + parts[0]);
    }
    
    if (savedToken == null) return null;
    String savedParts[] = savedToken.split(":", 4);
    if (savedParts.length != 4) return null;
    
    if (!savedParts[0].equals(parts[0]) || !savedParts[1].equals(parts[1])) {
      return null;
    }
    
    long createAt = Long.valueOf(savedParts[3]);
    long now = System.currentTimeMillis()/1000;
    if (createAt + maxAge/2 < now) {
      try (Jedis c = jedisPool.getResource()) {
        c.setex(KEY_PREFIX + parts[0], maxAge,
                String.join(":", token, savedParts[2], Long.toString(now)));
      }
    }

    long uid = Long.valueOf(parts[0]);
    int  perm = savedParts[2].length() == 0 ? Integer.MIN_VALUE : Integer.valueOf(savedParts[2]);
    return new User(uid, perm);
  }

  @Override
  public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
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

    String role = "ROLE_PERM";
    if (user.getPerm() != Integer.MIN_VALUE) role = role + String.valueOf(user.getPerm());

    List<GrantedAuthority> grantedAuths = Arrays.asList(new SimpleGrantedAuthority(role));
    return new RememberMeAuthenticationToken("N/A", user, grantedAuths);
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
