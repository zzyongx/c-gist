package commons.saas;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import com.qcloud.PicCloud;
import com.qcloud.UploadResult;

public class QCloudImageService extends ImageService {
  private String bucket;
  private int    appid;
  private String secretId;
  private String secretKey;
  
  public QCloudImageService(String bucket, int appid, String secretId, String secretKey) {
    this.bucket    = bucket;
    this.appid     = appid;
    this.secretId  = secretId;
    this.secretKey = secretKey;
  }

  public Image add(InputStream in) {
    UploadResult result;
    try {
      result = new UploadResult();
      PicCloud pc = new PicCloud(appid, secretId, secretKey, bucket);
      if (pc.upload(in, result) != 0) {
        throw new ImageException("upload file error");
      }
    } catch (Exception e) {
      throw new ImageException(e);
    }
    

    Image image = new Image();
    image.setId(result.fileId);
    image.setUrl(result.downloadUrl);
    image.setWidth(result.width);
    image.setHeight(result.height);
    return image;
  }

  public Image add(byte[] bytes) {
    return add(new ByteArrayInputStream(bytes));
  }

  public boolean delete(String id) {
    PicCloud pc = new PicCloud(appid, secretId, secretKey, bucket);
    return pc.delete(id) == 0;
  }
}
