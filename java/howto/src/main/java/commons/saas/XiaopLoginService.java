package commons.saas;

import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisPool;

@JsonIgnoreProperties(ignoreUnknown = true)
class HttpRet {
  public int      status;
  public UserInfo data;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UserInfo {
  public String uid;
  public String name;
  public String tel;
  public String dept;
  public String seat;
}

public class XiaopLoginService extends LoginService {
  public static final String API =
    "http://puboa.sogou-inc.com/moa/sylla/mapi/pns/checktoken?token={token}";
    
  public XiaopLoginService(JedisPool jedisPool) {
    super(jedisPool);
  }
    
  protected User doLogin(String tmpToken) {
    HttpRet ret = new RestTemplate().getForObject(API, HttpRet.class, tmpToken);
    if (ret.status != 0) return null;
    
    LoginService.User user = new LoginService.User();
    user.setOpenId("xiaop_" + ret.data.uid);
    user.setName(ret.data.name);
    user.setHeadImg("https://puboa.sogou-inc.com/moa/sylla/mapi/portrait?uid=" + ret.data.uid);

    HashMap<String, String> map = new HashMap<>();
    map.put("tel", ret.data.tel);
    map.put("dept", ret.data.dept);
    map.put("seat", ret.data.seat);
    user.setInfo(map);
    
    return user;
  }
}
