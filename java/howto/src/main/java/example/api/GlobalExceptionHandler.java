package example.api;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import com.google.common.base.Throwables;
import org.springframework.beans.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.dao.DataAccessException;
import example.model.*;

@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(RuntimeException.class)
  @ResponseBody
  public ApiResult internalServerError(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.INTERNAL_ERROR, e.toString());
  }

  @ExceptionHandler(DataAccessException.class)
  @ResponseBody
  public ApiResult dataAccessExcption(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.INTERNAL_ERROR, "interal db error");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public ApiResult httpMessageNotReadableException(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(ServletRequestBindingException.class)
  @ResponseBody
  public ApiResult servletRequestBindingException(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseBody
  public ApiResult constraintViolationException(ConstraintViolationException e) {
    List<String> errors = e.getConstraintViolations().stream()
      .map(ConstraintViolation::getMessage).collect(Collectors.toList());
    return new ApiResult<List>(Errno.BAD_REQUEST, errors);
  }

  @ExceptionHandler(ApiResult.AsException.class)
  @ResponseBody
  public ApiResult apiResultAsException(ApiResult.AsException e) {
    return e.get();
  }

  @ExceptionHandler(Errno.BadRequestException.class)
  @ResponseBody
  public ApiResult badRequestException(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(Errno.InternalErrorException.class)
  @ResponseBody
  public ApiResult internalErrorException(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.INTERNAL_ERROR, e.toString());
  }

  @ExceptionHandler(TypeMismatchException.class)
  @ResponseBody
  public ApiResult typeMismatchException(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(ResourceAccessException.class)
  @ResponseBody
  public ApiResult resourceAccessException(Exception e) {
    e.printStackTrace();
    return new ApiResult(Errno.INTERNAL_ERROR, e.getMessage());
  }
}
