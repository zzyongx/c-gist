package example.api;

import java.util.*;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import commons.utils.*;

@Api(
  name = "RestTemplate",
  description = "Demonstrate how to use RestTemplate"
)
@RestController
@RequestMapping("/api/restclient")
public class RestClient {
  public static class Version {
    private String version;
    private String license;

    public String getVersion() {
      return version;
    }
    public void setVersion(String version) {
      this.version = version;
    }

    public String getLicense() {
      return license;
    }
    public void setLicense(String license) {
      this.license = license;
    }
  }

  @Autowired RestTemplate restTemplate;

  private static final String HOST = "http://127.0.0.1/";
  private static final String VERSION_URI   = HOST + "/api/restserver/version";
  private static final String COLOR_URI     = HOST + "/api/restserver/color";
  private static final String COLOR_URI_TPL = HOST + "/api/restserver/color/{colors}";

  @ApiMethod(description = "get version")
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public ApiResult version() {
    Version version = restTemplate.getForObject(VERSION_URI, Version.class);
    return new ApiResult<Version>(version);
  }

  @ApiMethod(description = "update version, it will be failed")
  @RequestMapping(value = "/version", method = RequestMethod.PUT)
  public ApiResult testPostForObject() {
    Version version = new Version();
    version.setVersion("1.1");
    version.setLicense("GPL");
    restTemplate.postForObject(VERSION_URI, version, Version.class);
    return ApiResult.ok();
  }

  public static class StringMap extends HashMap<String, String> {}

  @ApiMethod(description = "get color table")
  @RequestMapping(value = "/color", method = RequestMethod.GET)
  public ApiResult colorTable() {
    Map<String, String> colors = restTemplate.getForObject(COLOR_URI, StringMap.class);
    return new ApiResult<Map>(colors);
  }

  @ApiMethod(description = "get specific color")
  @RequestMapping(value = "/color/{colorNames}", method = RequestMethod.GET)
  public ApiResult color(
    @ApiPathParam(name = "colors", description = "color name list", format = "c1,c2")
    @PathVariable String colorNames) {
    Map colors = restTemplate.getForObject(COLOR_URI_TPL, Map.class, colorNames);
    return new ApiResult<Map>(colors);
  }

  // /color/name:Gray,code:808080
  @ApiMethod(description = "add color item")
  @RequestMapping(value = "/color/{colors}", method = RequestMethod.PUT)
  public ApiResult addColorByPath(
    @ApiPathParam(name = "colors", description = "color list")
    @PathVariable String colors) {
    restTemplate.put(COLOR_URI_TPL, "", colors);
    return ApiResult.ok();
  }

  // name:Gray,code:#808080;name:Navy,code:#000080
  @ApiMethod(description = "add color item")
  @RequestMapping(value = "/color", method = RequestMethod.PUT)
  public ApiResult addColorByBody(
    @ApiBodyObject @RequestBody String colors) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("application", "objectlist"));
    HttpEntity<String> entity = new HttpEntity<String>(colors, headers);

    restTemplate.exchange(COLOR_URI, HttpMethod.PUT, entity, Void.class);
    return ApiResult.ok();
  }
}
