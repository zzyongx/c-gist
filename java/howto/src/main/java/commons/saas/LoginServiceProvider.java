package commons.saas;

public class LoginServiceProvider {
  public static enum Name {
    XiaoP, WeiXin
  }

  private LoginService xiaop;
  private LoginService weixin;

  public LoginService get(Name name) {
    switch (name) {
    case XiaoP: return xiaop;      
    case WeiXin: return weixin;
    default: return null;
    }
  }

  public LoginService get(String openId) {
    if (openId.startsWith("xiaop_")) return xiaop;
    else if (openId.startsWith("weixin_")) return weixin;
    return null;
  }

  public void register(Name name, LoginService service) {
    switch (name) {
    case XiaoP: xiaop = service; break;
    case WeiXin: weixin = service; break;
    }
  }
}
