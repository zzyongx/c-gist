package commons.spring;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.StreamUtils;

@ManagedResource(objectName = "bean:name=loggerFilter")
public class LoggerFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(LoggerFilter.class);
  
  private boolean logHttpGet;
  private boolean logHttpPost;
  private boolean logHttpPut;
  private boolean logHttpDelete;
  private boolean logError;

  public LoggerFilter(Environment env) {
    String def = "false";
    if (env.acceptsProfiles("dev") || env.acceptsProfiles("test")) {
      def = "true";
    }

    logHttpGet    = Boolean.parseBoolean(env.getProperty("logfilter.get", "false"));
    logHttpPost   = Boolean.parseBoolean(env.getProperty("logfilter.post", def));
    logHttpPut    = Boolean.parseBoolean(env.getProperty("logfilter.put", def));
    logHttpDelete = Boolean.parseBoolean(env.getProperty("logfilter.delete", def));
    logError      = Boolean.parseBoolean(env.getProperty("logfilter.error", "true"));
  }

  @ManagedAttribute(description="The logHttpGet Attribute", defaultValue="false")
  public boolean getLogHttpGet() {
    return this.logHttpGet;
  }
  
  @ManagedAttribute(description="The logHttpGet Attribute")
  public void setLogHttpGet(boolean logHttpGet) {
    this.logHttpGet = logHttpGet;
  }

  @ManagedAttribute(description="The logHttpPost Attribute", defaultValue="false")
  public boolean getLogHttpPost() {
    return this.logHttpPost;
  }
  
  @ManagedAttribute(description="The logHttpPost Attribute")
  public void setLogHttpPost(boolean logHttpPost) {
    this.logHttpPost = logHttpPost;
  }

  @ManagedAttribute(description="The logHttpPut Attribute", defaultValue="false")
  public boolean setLogHttpPut() {
    return this.logHttpPut;
  }
  
  @ManagedAttribute(description="The logHttpPut Attribute")
  public void setLogHttpPut(boolean logHttpPut) {
    this.logHttpPut = logHttpPut;
  }

  @ManagedAttribute(description="The logHttpDelete Attribute", defaultValue="false")
  public boolean getLogHttpDelete() {
    return this.logHttpDelete;
  }
  
  @ManagedAttribute(description="The logHttpDelete Attribute")
  public void setLogHttpDelete(boolean logHttpDelete) {
    this.logHttpDelete = logHttpDelete;
  }
  
  @ManagedAttribute(description="The logError Attribute", defaultValue="false")
  public boolean getLogError() {
    return this.logError;
  }

  @ManagedAttribute(description="The logError Attribute")
  public void setLogError(boolean logError) {
    this.logError = logError;
  }
  
  public void init(FilterConfig arg) throws ServletException {
    // nothing to do
  }
  
  public void destroy() {
    // nothing to do
  }

  boolean ifLog(String method, HttpServletRequest req) {
    if (logHttpGet && method.equals("GET") ||
        logHttpDelete && method.equals("DELETE")) {
      return true;
    } else if (logHttpPost && method.equals("POST") ||
               logHttpPut && method.equals("PUT")) {
      String contentType = req.getContentType();
      if (contentType != null &&
          (contentType.startsWith("application/x-www-form-urlencoded") ||
           contentType.startsWith("multipart/form-data") ||
           contentType.startsWith("application/json") ||
           contentType.startsWith("text/plain"))) {
        return true;
      }
    }
    return false;
  }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;
    
    String method = req.getMethod();
    boolean log = ifLog(method, req);
   
    ServletRequest reqWrap   = request;
    ServletResponse respWrap = response;
    ResettableStreamHttpServletRequest  resetableReq = null;
    ResettableStreamHttpServletResponse resetableResp = null;
    String reqBody = "-";
    String respBody = null;

    if (log) {
      if (method.equals("POST") || method.equals("PUT")) {
        resetableReq = new ResettableStreamHttpServletRequest(req);
        reqWrap = resetableReq;
      }
      resetableResp = new ResettableStreamHttpServletResponse(resp);
      respWrap = resetableResp;
    }

    if (logError) request.setAttribute("response__", resp);

    chain.doFilter(reqWrap, respWrap);
    
    if (log) {
      if (resetableReq != null) {
        // must call after doFilter
        byte[] bytes = resetableReq.getData();
        if (bytes == null) {
          reqBody = req.getParameterMap().toString();
        } else {
          reqBody = new String(bytes);
        }
      }
      
      byte[] bytes = resetableResp.getData();
      response.getOutputStream().write(bytes);
      if (bytes != null) respBody = new String(bytes);
    } else if (logError) {
      respBody = (String) request.getAttribute("ApiResultError");
    }

    if (log || logError && respBody != null) {
      String queryStr = req.getQueryString();
      if (queryStr == null) queryStr = "-";

      if (respBody == null) respBody = "-";
      logger.warn("{} {} {} {} {}", method, req.getRequestURI(), queryStr, reqBody, respBody);
    }
  }

  private static class ResettableStreamHttpServletRequest extends
    HttpServletRequestWrapper {

    private byte rawData[];
    private ServletInputStreamImpl servletStream;

    public ResettableStreamHttpServletRequest(HttpServletRequest request) {
      super(request);
      this.servletStream = new ServletInputStreamImpl();
    }

    public byte[] getData() {
      return rawData;
    }      

    @Override
    public ServletInputStream getInputStream() throws IOException {
      rawData = StreamUtils.copyToByteArray(super.getInputStream());
      servletStream.setData(rawData);
      return servletStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    private static class ServletInputStreamImpl extends ServletInputStream {
      public ByteArrayInputStream stream;

      public void setData(byte[] data) {
        stream = new ByteArrayInputStream(data);
      }

      @Override
      public int read() throws IOException {
        return stream.read();
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public boolean isFinished() {
        return stream.available() > 0;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
      }
    }
  }

  private static class ResettableStreamHttpServletResponse extends
    HttpServletResponseWrapper {

    private ServletOutputStreamImpl servletStream;

    public ResettableStreamHttpServletResponse(HttpServletResponse response) {
      super(response);
      this.servletStream = new ServletOutputStreamImpl();
    }

    public byte[] getData() {
      return this.servletStream.stream.toByteArray();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      return servletStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      return new PrintWriter(servletStream.stream, true);
    }

    private static class ServletOutputStreamImpl extends ServletOutputStream {
      private ByteArrayOutputStream stream;

      public ServletOutputStreamImpl() {
        stream = new ByteArrayOutputStream();
      }

      @Override
      public void write(int param) throws IOException {
        stream.write(param);
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setWriteListener(WriteListener writeListener) {
      }
    }
  }
}

