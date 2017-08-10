package commons.spring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

class ClientHttpResponseInvocationHandler implements InvocationHandler {
  final ClientHttpResponse proxied;
  ByteArrayInputStream inputStream;

  public ClientHttpResponseInvocationHandler(ClientHttpResponse response, byte[] data) {
    this.proxied = response;
    this.inputStream = new ByteArrayInputStream(data);
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName() == "getBody") {
      return inputStream;
    }
    return method.invoke(proxied, args);
  }
}

public class RestTemplateFilter implements ClientHttpRequestInterceptor {
  static final Logger logger = LoggerFactory.getLogger(RestTemplateFilter.class);

  private Map<String, List<String>> newHeaders;
  private Map<String, List<String>> replaceHeaders;

  public RestTemplateFilter addHeader(String name, List<String> value) {
    if (newHeaders == null) newHeaders = new HashMap<>();
    newHeaders.put(name, value);
    return this;
  }

  public RestTemplateFilter replaceHeader(String name, List<String> value) {
    if (replaceHeaders == null) replaceHeaders = new HashMap<>();
    replaceHeaders.put(name, value);
    return this;
  }

  @Override
  public ClientHttpResponse intercept(
    HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
    throws IOException {

    HttpHeaders headers = request.getHeaders();
    if (newHeaders != null) {
      newHeaders.entrySet().forEach(e -> headers.put(e.getKey(), e.getValue()));
    }

    if (replaceHeaders != null) {
      replaceHeaders.entrySet().forEach(e->headers.replace(e.getKey(), e.getValue()));
    }

    if (!logger.isDebugEnabled()) return execution.execute(request, body);

    long start = System.currentTimeMillis();
    ClientHttpResponse response = execution.execute(request, body);
    long end = System.currentTimeMillis();

    byte[] bytes = StreamUtils.copyToByteArray(response.getBody());

    ClientHttpResponse proxy = (ClientHttpResponse) Proxy.newProxyInstance(
      RestTemplateFilter.class.getClassLoader(),
      new Class[] { ClientHttpResponse.class },
      new ClientHttpResponseInvocationHandler(response, bytes));

    String input = body == null || body.length == 0 ? "-" : new String(body);
    String output = bytes == null || bytes.length == 0 ? "-" : new String(bytes);

    logger.debug("{} {} {} {} {} {}", end - start, request.getMethod(), request.getURI(), input,
                 response.getRawStatusCode(), output);
    return proxy;
  }
}
