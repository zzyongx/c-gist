package commons.spring.controller;

import java.net.URISyntaxException;
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
import org.springframework.dao.CannotAcquireLockException;
import commons.utils.*;

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
    return ApiResult.internalError(e.toString());
  }

  @ExceptionHandler(URISyntaxException.class)
  @ResponseBody
  public ApiResult uriSyntaxException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return ApiResult.badRequest(e.toString());
  }

  @ExceptionHandler(DataAccessException.class)
  @ResponseBody
  public ApiResult dataAccessExcption(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return ApiResult.internalError("interal db error");
  }

  @ExceptionHandler(CannotAcquireLockException.class)
  @ResponseBody
  public ApiResult cannotAcquireLockException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return ApiResult.internalError(e.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public ApiResult httpMessageNotReadableException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return ApiResult.badRequest(e.toString());
  }

  @ExceptionHandler(ServletRequestBindingException.class)
  @ResponseBody
  public ApiResult servletRequestBindingException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return ApiResult.badRequest(e.toString());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseBody
  public ApiResult constraintViolationException(ConstraintViolationException e) {
    List<String> errors = e.getConstraintViolations().stream()
      .map(ConstraintViolation::getMessage).collect(Collectors.toList());

    logger.error("{} throws {}", getContext(), errors);
    return new ApiResult<List>(BaseErrno.BAD_REQUEST, errors);
  }

  @ExceptionHandler(ApiResult.AsException.class)
  @ResponseBody
  public ApiResult apiResultAsException(ApiResult.AsException e) {
    return e.get();
  }

  @ExceptionHandler(TypeMismatchException.class)
  @ResponseBody
  public ApiResult typeMismatchException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return ApiResult.badRequest(e.toString());
  }

  @ExceptionHandler(ResourceAccessException.class)
  @ResponseBody
  public ApiResult resourceAccessException(Exception e) {
    logger.error("{} throws {}", getContext(), Throwables.getStackTraceAsString(e));
    return ApiResult.internalError(e.getMessage());
  }
}
