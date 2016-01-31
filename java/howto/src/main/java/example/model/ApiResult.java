package example.model;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.validation.*;

public class ApiResult<Data> {
  private int code;
  private String message;

  @JsonInclude(Include.NON_NULL)
  Data data;

  public ApiResult() {
    this(Errno.OK);
  }

  public ApiResult(int code) {
    this(code, Errno.getMessage(code));
  }

  public ApiResult(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public ApiResult(Data data) {
    this(Errno.OK, Errno.getMessage(Errno.OK), data);
  }
  
  public ApiResult(int code, Data data) {
    this(code, Errno.getMessage(code), data);
  }

  public static ApiResult ok() {
    return new ApiResult();
  }

  public static ApiResult badRequest(String msg) {
    return new ApiResult(Errno.BAD_REQUEST, msg);
  }

  public static ApiResult unAuthorized() {
    return new ApiResult(Errno.UNAUTHORIZED);
  }

  public static ApiResult forbidden() {
    return new ApiResult(Errno.FORBIDDEN);
  }
  
  public static ApiResult notFound() {
    return new ApiResult(Errno.NOT_FOUND);
  }

  public static ApiResult notAccept(String msg) {
    return new ApiResult(Errno.NOT_ACCEPT, msg);
  }

  public static ApiResult internalError(String msg) {
    return new ApiResult(Errno.INTERNAL_ERROR, msg);
  }

  public static ApiResult notImplement() {
    return new ApiResult(Errno.NOT_IMPLEMENT);
  }

  public static ApiResult serviceUnavailable(String msg) {
    return new ApiResult(Errno.SERVICE_UNAVAILABLE, msg);
  }

  public static Errno.BadRequestException badRequestException() {
    return new Errno.BadRequestException();
  }  

  public static ApiResult bindingResult(BindingResult bindingResult) {
    Map<String, String> map = new HashMap<>();
    for(FieldError error : bindingResult.getFieldErrors()){
      map.put(error.getField(), error.getDefaultMessage());
    }
    return new ApiResult<Map>(Errno.BAD_REQUEST, map);
  }
  
  public ApiResult(int code, String message, Data data) {
    this.code = code;
    this.message = message;
    this.data = data;
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
}
