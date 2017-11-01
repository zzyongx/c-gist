package commons.utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.jsondoc.core.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.validation.*;
import org.springframework.web.context.request.*;

@ApiObject(name = "ApiResult", description = "ApiResult")
public class ApiResult<Data> {
  @ApiObjectField(description = "error code")
  private int code;

  @ApiObjectField(description = "error message")
  private String message;

  @ApiObjectField(description = "payload")
  @JsonInclude(Include.NON_NULL)
  Data data;

  public ApiResult() {
    this(BaseErrno.OK);
  }

  public ApiResult(int code) {
    this(code, BaseErrno.getMessage(code));
  }

  public static HttpServletResponse getHttpServletRespons() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                                  .getRequestAttributes()).getRequest();
    return (HttpServletResponse) request.getAttribute("response__");
  }

  private void setErrorHint() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                                  .getRequestAttributes()).getRequest();
    String error     = String.valueOf(code) + ":" + message;
    String errHeader = error.length() > 90 ? error.substring(0, 90) : error;

    HttpServletResponse response = (HttpServletResponse) request.getAttribute("response__");
    if (response != null) response.setHeader("ApiResultError", errHeader);
    request.setAttribute("ApiResultError", error);
  }

  private void clearErrorHint() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                                  .getRequestAttributes()).getRequest();

    HttpServletResponse response = (HttpServletResponse) request.getAttribute("response__");
    if (response != null) response.setHeader("ApiResultError", "");
    request.removeAttribute("ApiResultError");
  }

  public static ApiResult wrap(ApiResult rc) {
    if (rc.code != BaseErrno.OK) rc.setErrorHint();
    else rc.clearErrorHint();
    return rc;
  }

  public ApiResult(int code, String message) {
    this(code, message, null);
  }

  public ApiResult(Data data) {
    this(BaseErrno.OK, BaseErrno.getMessage(BaseErrno.OK), data);
  }

  public ApiResult(int code, Data data) {
    this(code, BaseErrno.getMessage(code), data);
  }

  public static class AsException extends RuntimeException {
    ApiResult result;
    public AsException(ApiResult result) {
      this.result = result;
    }
    public ApiResult get() {
      return result;
    }
  }

  public RuntimeException toException() {
    return new AsException(this);
  }

  public static ApiResult ok() {
    return new ApiResult();
  }

  public static ApiResult badRequest(String msg) {
    return new ApiResult(BaseErrno.BAD_REQUEST, msg);
  }

  public static ApiResult unAuthorized() {
    return new ApiResult(BaseErrno.UNAUTHORIZED);
  }

  public static ApiResult forbidden() {
    return new ApiResult(BaseErrno.FORBIDDEN);
  }

  public static ApiResult forbidden(String msg) {
    return new ApiResult(BaseErrno.FORBIDDEN, msg);
  }

  public static ApiResult notFound() {
    return new ApiResult(BaseErrno.NOT_FOUND);
  }

  public static ApiResult notFound(String msg) {
    return new ApiResult(BaseErrno.NOT_FOUND, msg);
  }

  public static ApiResult notAccept(String msg) {
    return new ApiResult(BaseErrno.NOT_ACCEPT, msg);
  }

  public static ApiResult internalError(String msg) {
    return new ApiResult(BaseErrno.INTERNAL_ERROR, msg);
  }

  public static ApiResult notImplement() {
    return new ApiResult(BaseErrno.NOT_IMPLEMENT);
  }

  public static ApiResult serviceUnavailable(String msg) {
    return new ApiResult(BaseErrno.SERVICE_UNAVAILABLE, msg);
  }

  public static ApiResult bindingResult(BindingResult bindingResult) {
    Map<String, String> map = new HashMap<>();
    for(FieldError error : bindingResult.getFieldErrors()){
      map.put(error.getField(), error.getDefaultMessage());
    }
    return new ApiResult<Map>(BaseErrno.BAD_REQUEST, map);
  }

  public ApiResult(int code, String message, Data data) {
    this.code = code;
    this.message = message;
    this.data = data;

    if (this.code != BaseErrno.OK) setErrorHint();
    else clearErrorHint();
  }

  @JsonIgnore
  public boolean isOk() {
    return this.code == BaseErrno.OK;
  }

  public int getCode() {
    return code;
  }
  public void setCode(int code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }

  public Data getData() {
    return data;
  }
  public void setData(Data data) {
    this.data = data;
  }

  public ApiResult<Data> setCacheControl(int second) {
    HttpServletResponse response = getHttpServletRespons();
    if (response == null) return this;

    CacheControl cc = CacheControl.maxAge(second, TimeUnit.SECONDS).cachePrivate();
    response.setHeader("Cache-Control", cc.getHeaderValue());
    return this;
  }
}
