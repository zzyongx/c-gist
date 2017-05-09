package commons.saas;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.nio.charset.Charset;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisPool;
import commons.utils.DigestHelper;
import commons.spring.LooseGsonHttpMessageConverter;

class V5Phone {
  public String nationcode;
  public String mobile;

  public V5Phone() {}
  public V5Phone(String phone) {
    this.mobile = phone;
    this.nationcode = "86";
  }
}

class V5SendSmsReqBody {
  public V5Phone   tel;
  public int       type = 0;
  public String    msg;
  public String    sig;
  public long      time;
  public String    extend = "";
  public String    ext = "";
}

@JsonIgnoreProperties(ignoreUnknown = true)
class V5SendSmsRespBody {
  public int    result;
  public String errmsg;
}

public class QCloudSmsV5Service extends SmsService {
  private static final Charset charset = Charset.forName("UTF-8");
  private String appkey;
  private String uri;
  private RestTemplate rest;

  public QCloudSmsV5Service(RestTemplate rest, String appid, String appkey, JedisPool jedisPool) {
    super(jedisPool);
    this.rest = rest;
    this.appkey = appkey;
    this.uri = "https://yun.tim.qq.com/v5/tlssmssvr/sendsms?sdkappid=" + appid;
  }

  protected void sendInternal(String phone, String msg) {
    String random = String.valueOf(ThreadLocalRandom.current().nextInt(10000000, 99999999));

    V5SendSmsReqBody reqBody = new V5SendSmsReqBody();
    reqBody.tel = new V5Phone(phone);
    reqBody.msg = msg;
    reqBody.time = System.currentTimeMillis() / 1000L;
    reqBody.sig = DigestHelper.sha256(
      String.format("appkey=%s&random=%s&time=%d&mobile=%s",
                    appkey, random, reqBody.time, phone).getBytes(charset));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<V5SendSmsReqBody> reqEntity = new HttpEntity<>(reqBody, headers);

    ResponseEntity<V5SendSmsRespBody> respEntity = rest.exchange(
      uri + "&random=" + random, HttpMethod.POST, reqEntity, V5SendSmsRespBody.class);

    if (respEntity.getStatusCode() != HttpStatus.OK) {
      throw new SmsException("couldn't connect sms server");
    }

    V5SendSmsRespBody respBody = respEntity.getBody();
    if (respBody.result != 0) {
      throw new SmsException(respBody.errmsg);
    }
  }
}
