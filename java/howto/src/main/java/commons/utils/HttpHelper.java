package commons.utils;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class HttpHelper {
  public static String getClientIp() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                                  .getRequestAttributes()).getRequest();
    return getClientIp(request);
  }

  public static String getClientIp(HttpServletRequest request) {
    String header = request.getHeader("x-forwarded-for");
    if (header != null) {
      String part[] = header.split(",");
      if (part.length == 1) {
        return header;
      } else {
        return part[part.length-1].trim();
      }
    } else {
      return request.getRemoteAddr();
    }
  }
}
