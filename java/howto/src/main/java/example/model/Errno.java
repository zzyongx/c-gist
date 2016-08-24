package example.model;

import org.jsondoc.core.annotation.*;

@ApiObject(name = "Errno", description = "error code")
public class Errno {
  @ApiObjectField(description = "0")
  public static final int OK                  = 0;

  @ApiObjectField(description = "400")
  public static final int BAD_REQUEST         = 400;

  @ApiObjectField(description = "401")
  public static final int UNAUTHORIZED        = 401;

  @ApiObjectField(description = "403")
  public static final int FORBIDDEN           = 403;

  @ApiObjectField(description = "404")
  public static final int NOT_FOUND           = 404;

  @ApiObjectField(description = "406")
  public static final int NOT_ACCEPT          = 406;

  @ApiObjectField(description = "500")
  public static final int INTERNAL_ERROR      = 500;

  @ApiObjectField(description = "501")
  public static final int NOT_IMPLEMENT       = 501;

  @ApiObjectField(description = "503")
  public static final int SERVICE_UNAVAILABLE = 503;

  public static String getMessage(int code) {
    switch (code) {
    case OK: return "OK";
    case BAD_REQUEST: return "bad request";
    case UNAUTHORIZED: return "unauthorized";
    case NOT_FOUND: return "entity not found";
    case NOT_ACCEPT: return "not acceptable";
    case INTERNAL_ERROR: return "internal server error";
    case SERVICE_UNAVAILABLE: return "service unavailable";
    default: return "";
    }
  }

  public static class BadRequestException extends RuntimeException {
  }

  public static class InternalErrorException extends RuntimeException {
    public InternalErrorException(Throwable throwable) {
      super(throwable);
    }
  }
}
