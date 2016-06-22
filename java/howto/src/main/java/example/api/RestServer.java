package example.api;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;
import org.jsondoc.core.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.WebDataBinder;
import commons.spring.AnalogueJsonObjectFormatter;

@Api(
  name = "rest server",
  description = "mock rest server"
)
@RestController
@RequestMapping("/api/restserver")
public class RestServer {
  Map<String, String> colorTable = new ConcurrentHashMap<String, String>(){{
      put("Black",  "#000000");
      put("White",  "#FFFFFF");
      put("Red",    "#FF0000");
      put("Lime",   "#00FF00");
      put("Blue",   "#0000FF");
      put("Yellow", "#FFF000");
      put("Silver", "#C0C0C0");
    }};

  final static String versionInfo = "{`version`: `1.2.3`, `license`: `Apache`}".replace('`', '"');

  @ApiMethod(description = "get version info")
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public @ResponseBody String version() {
    return versionInfo;
  }

  @ApiMethod(description = "get color table")
  @RequestMapping(value = "/color", method = RequestMethod.GET)
  public Map<String, String> colorTable() {
    return colorTable;
  }

  @ApiMethod(description = "get specific color")
  @RequestMapping(value = "/color/{colors}", method = RequestMethod.GET)
  public @ResponseBody String color(
    @ApiPathParam(name = "colors", description = "color name list", format = "c1,c2")
    @PathVariable List<String> colors) {
    List<String> colorCodes = new ArrayList<>();
    for (String color : colors) {
      String code = colorTable.get(color);
      colorCodes.add(code == null ? "" : code);
    }
    return String.join(",", colorCodes);
  }

  // /color/name:Gray,code:808080
  @ApiMethod(description = "add color item")
  @RequestMapping(value = "/color/{color}", method = RequestMethod.PUT)
  public void addColorByPath(
    @ApiPathParam(name = "color", description = "color")
    @PathVariable Color color) {
    colorTable.put(color.name, "#" + color.code);
  }

  public static class ColorList extends ArrayList<Color> {}

  // name:Gray,code:#808080;name:Navy,code:#000080
  @ApiMethod(description = "add color item")
  @RequestMapping(value = "/color", method = RequestMethod.PUT)
  public void addColorByBody(
    @ApiBodyObject @RequestBody ColorList colors) {
    for (Color color : colors) {
      colorTable.put(color.name, color.code);
    }
  }

  /* very complex and not necessary, for demonstration only */
  public static class Color {
    public String name;
    public String code;
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.addCustomFormatter(new AnalogueJsonObjectFormatter<Color>(Color.class), Color.class);
  }
}
