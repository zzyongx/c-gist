package commons.saas;

import java.util.Arrays;
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

class Phone {
  public String nationcode;
  public String phone;
  
  public Phone() {}
  public Phone(String phone) {
    this.phone = phone;
    this.nationcode = "86";
  }
}

class SendSmsReqBody {
  public Phone   tel;
  public String  msg;
  public String  sig;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SendSmsRespBody {
  public int    result;
  public String errmsg;
}

public class QCloudSmsService extends SmsService {
  private static final Charset charset = Charset.forName("UTF-8");
  private String appkey;
  private String uri;

  public QCloudSmsService(String appid, String appkey, JedisPool jedisPool) {
    super(jedisPool);
    this.appkey = appkey;
    this.uri = "https://yun.tim.qq.com/v3/tlssmssvr/sendsms?sdkappid=" + appid;
  }

  protected void sendInternal(String phone, String msg) {
    SendSmsReqBody reqBody = new SendSmsReqBody();
    reqBody.tel = new Phone(phone);
    reqBody.msg = msg;
    reqBody.sig = DigestHelper.md5((appkey + phone).getBytes(charset));

    RestTemplate restTemplate = new RestTemplate();

    LooseGsonHttpMessageConverter converter[] = { new LooseGsonHttpMessageConverter() };
    restTemplate.setMessageConverters(Arrays.asList(converter));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<SendSmsReqBody> reqEntity = new HttpEntity<>(reqBody, headers);

    ResponseEntity<SendSmsRespBody> respEntity = restTemplate.exchange(
      uri, HttpMethod.POST, reqEntity, SendSmsRespBody.class);

    if (respEntity.getStatusCode() != HttpStatus.OK) {
      throw new SmsException("couldn't connect sms server");
    }

    SendSmsRespBody respBody = respEntity.getBody();
    if (respBody.result != 0) {
      throw new SmsException(respBody.errmsg);
    }
  }
}
