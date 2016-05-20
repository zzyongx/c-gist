package commons.saas;

public class ImageException extends RuntimeException {
  public ImageException(String msg) {
    super(msg);
  }
  public ImageException(Throwable cause) {
    super(cause);
  }
}
