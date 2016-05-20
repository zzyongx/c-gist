package commons.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.awt.Color;
import javax.imageio.ImageIO;
import org.patchca.color.SingleColorFactory;
import org.patchca.background.SingleColorBackgroundFactory;
import org.patchca.filter.predefined.DefaultRippleFilterFactory;
import org.patchca.font.RandomFontFactory;
import org.patchca.service.Captcha;
import org.patchca.service.ConfigurableCaptchaService;
import org.patchca.word.DefaultRandomWordFactory;

public class PatchcaHelper {
  private static ConfigurableCaptchaService captchaService = new ConfigurableCaptchaService();
  private static DefaultRandomWordFactory wordFactory =
    new DefaultRandomWordFactory("123456789ABCDEFGHJKLMNPQRSTUVWXYZ", 5);
  private static DefaultRippleFilterFactory filterFactory = new DefaultRippleFilterFactory();
  private static RandomFontFactory fontFactory = new RandomFontFactory();

  static {
    filterFactory.setLineNum(0);
    captchaService.setFilterFactory(filterFactory);
    
    fontFactory.setMinSize(48);
    fontFactory.setMaxSize(48);
    captchaService.setFontFactory(fontFactory);

    captchaService.setColorFactory(new SingleColorFactory(new Color(25, 60, 170)));
    captchaService.setBackgroundFactory(new SingleColorBackgroundFactory(new Color(255, 255, 255)));
    captchaService.setWordFactory(wordFactory);
    captchaService.setWidth(160);
    captchaService.setHeight(56);
  }
  
  public static String get(OutputStream os) {
    Captcha captcha = captchaService.getCaptcha();
    try {
      ImageIO.write(captcha.getImage(), "png", os);
      return captcha.getChallenge();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }  
}
