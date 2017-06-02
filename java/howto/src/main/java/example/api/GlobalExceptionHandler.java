package example.api;

import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.dao.DataAccessException;
import example.model.*;

@ControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(
    "commons.spring.GlobalExceptionHandler");

  public static String getContext() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                                  .getRequestAttributes()).getRequest();
    String method = request.getMethod();

    String queryStr = request.getQueryString();
    if (queryStr == null) queryStr = "-";

    String userId = (String) request.getAttribute("RmsUid");
    if (userId == null) userId = "-";

    String reqBody;
    if ("POST".equals(method) || "PUT".equals(method)) {
      reqBody = request.getParameterMap().toString();
    } else {
      reqBody = "-";
    }

    return method + " " + request.getRequestURI() + " " +
      userId + " " + queryStr + " " + reqBody;
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseBody
  public ApiResult internalServerError(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return new ApiResult(Errno.INTERNAL_ERROR, e.toString());
  }

  @ExceptionHandler(DataAccessException.class)
  @ResponseBody
  public ApiResult dataAccessExcption(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return new ApiResult(Errno.INTERNAL_ERROR, "interal db error");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public ApiResult httpMessageNotReadableException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(ServletRequestBindingException.class)
  @ResponseBody
  public ApiResult servletRequestBindingException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseBody
  public ApiResult constraintViolationException(ConstraintViolationException e) {
    List<String> errors = e.getConstraintViolations().stream()
      .map(ConstraintViolation::getMessage).collect(Collectors.toList());

    logger.error("{} throws {}", getContext(), errors);
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
    logger.error("{} throws {}", getContext(), e);
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(Errno.InternalErrorException.class)
  @ResponseBody
  public ApiResult internalErrorException(Exception e) {
    logger.error("{} throws {}", getContext(), e);
    return new ApiResult(Errno.INTERNAL_ERROR, e.toString());
  }

  @ExceptionHandler(TypeMismatchException.class)
  @ResponseBody
  public ApiResult typeMismatchException(Exception e) {
    logger.error("{} throws {}", getContext(), e);
    return new ApiResult(Errno.BAD_REQUEST, e.toString());
  }

  @ExceptionHandler(ResourceAccessException.class)
  @ResponseBody
  public ApiResult resourceAccessException(Exception e) {
    logger.error("{} throws {}", getContext(), e);
    return new ApiResult(Errno.INTERNAL_ERROR, e.getMessage());
  }
}
