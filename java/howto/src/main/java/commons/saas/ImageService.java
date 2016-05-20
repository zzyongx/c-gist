package commons.saas;

import java.io.InputStream;

public abstract class ImageService {
  public static class Image {
    private String id;
    private String url;
    private int    width;
    private int    height;

    public void setId(String id) {
      this.id = id;
    }
    public String getId() {
      return this.id;
    }

    public void setUrl(String url) {
      this.url = url;
    }
    public String getUrl() {
      return this.url;
    }

    public void setWidth(int width) {
      this.width = width;
    }
    public int getWidth() {
      return this.width;
    }

    public void setHeight(int height) {
      this.height = height;
    }
    public int getHeight() {
      return this.height;
    }
  }

  public abstract Image add(InputStream in);
  public abstract Image add(byte[] bytes);
  public abstract boolean delete(String id);
}
