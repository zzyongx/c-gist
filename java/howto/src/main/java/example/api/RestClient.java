package example.api;

import java.util.*;
import org.jsondoc.core.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import example.model.*;

@Api(
  name = "RestTemplate",
  description = "Demonstrate how to use RestTemplate"
)
@RestController
@RequestMapping("/api")
public class RestClient {
  public static class Version {
    private String version;
    private String license;

    public String getVersion() {
      return version;
    }
    public void setVersion() {
      this.version = version;
    }

    public String getLicense() {
      return license;
    }
    public void setLicense() {
      this.license = license;
    }
  }

  private RestTemplate restTemplate = new RestTemplate();
  private static final String HOST = "http://127.0.0.1:8888/";
  private static final String VERSION_URI   = HOST + "/api/restserver/version";
  private static final String COLOR_URI     = HOST + "/api/restserver/color";
  private static final String COLOR_URI_TPL = HOST + "/api/restserver/color/{colors}";
  
  @ApiMethod(description = "get version")
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public ApiResult version() {
    Version version = restTemplate.getForObject(VERSION_URI, Version.class);
    return new ApiResult<Version>(version);
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
