package example.api;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.validation.Valid;
import javax.servlet.http.*;
import javax.validation.constraints.*;
import org.hibernate.validator.constraints.NotBlank;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import example.model.*;
import example.config.*;

@RestController
@RequestMapping("/api")
public class EchoController {
  
  public static class Profile {
    @NotBlank(message = "name is required")
    public String name;
    
    @Pattern(regexp = "\\d{11,15}", message = "phone is required")
    public String phone;
    
    public List<String> favoriteColor;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    public LocalDateTime birthday;
    
    public void setName(String name) {
      this.name = name;
    }
    public void setPhone(String phone) {
      this.phone = phone;
    }
    public void setFavoriteColor(List<String> color) {
      this.favoriteColor = color;
    }
    public void setBirthday(LocalDateTime birthday) {
      this.birthday = birthday;
    }
  }
  
  // OptionalLong is not support now, use Optional<Long> instead of
  // curl dev:8180/api/power/5
  // curl dev:8180/api/power/5/3
  @RequestMapping(value = {"/power/{base}", "/power/{base}/{power}"}, method = RequestMethod.GET)
  public long getPower(@PathVariable long base, @PathVariable("power") Optional<Long> powerOpt) {
    long power = powerOpt.orElse(2L);
    return (long) Math.pow(base, power);
  }

  // curl dev:8180/api/upper/hello
  // curl dev:8180/api/upper?qs=world
  @RequestMapping(value = {"/upper", "/upper/{upper}"}, method = RequestMethod.GET)
  public String getUpper(
    @PathVariable Optional<String> upper,
    @RequestParam(required = false, defaultValue = "Hello world") String qs) {
    if (upper.isPresent()) return upper.get().toUpperCase();
    return qs.toUpperCase();
  }

  // curl dev:8180/api/books -H "Accept: text/plain"
  @RequestMapping(value = "/books", produces = "text/plain")
  public String getBooksText() {
    return "Mastering Spring MVC,Spring in Action";
  }

  // curl dev:8180/api/books
  @RequestMapping(value = "/books")
  public List<String> getBooks() {
    return Arrays.asList("Mastering Spring MVC", "Spring in Action");
  }

  // curl -X PUT dev:8180/api/profile -d '{"name": "zzyong", "phone": 12345678901}' -H "Content-Type: application/octet-stream"
  @RequestMapping(value = "/profile", method = RequestMethod.PUT,
                  consumes = "application/octet-stream")
  public void submitProfileStream(HttpServletRequest request, HttpServletResponse response)
    throws IOException {
    InputStream input = request.getInputStream();
    OutputStream output = response.getOutputStream();
    
    byte[] buffer = new byte[512];
    int n;
    while ((n = input.read(buffer)) > 0) {
      output.write(buffer, 0, n);
    }
  }

  // curl -X PUT dev:8180/api/profile -d '{"name": "zzyong", "phone": 12345678901}' -H "Content-Type: application/json"  
  @RequestMapping(value = "/profile", method = RequestMethod.PUT, consumes = "application/json")
  public String submitProfileJson(@RequestBody String json) {
    return json;
  }

  // curl -X PUT "dev:8180/api/profile" -d 'name=zzyong&phone=12345678901&favoriteColor=red,green' -vv
  // Content-Type: application/x-www-form-urlencoded
  // curl -X PUT "dev:8180/api/profile" -F 'name=zzyong' -F 'phone=12345678901' -F 'favoriteColor=red,green' -vv
  // Content-Type: multipart/form-data; boundary=------------------------c00a4e11cd9e4e67
  @RequestMapping(value = "/profile", method= RequestMethod.PUT)
  public ResponseEntity submitProfileForm(@Valid Profile profile, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      Map<String, String> map = new HashMap<>();
      for(FieldError error : bindingResult.getFieldErrors()){
        map.put(error.getField(), error.getDefaultMessage());
      }
      return new ResponseEntity<Map>(map, HttpStatus.BAD_REQUEST);      
    } else {
      return new ResponseEntity<Profile>(profile, HttpStatus.OK);
    }
  }

  // curl "dev:8180/api/profile" -H "user: bob" -H "password: secret" -vv
  // curl "dev:8180/api/profile" -H "Cookie: uid=btxaergfe" -vv
  @RequestMapping(value = "/profile", method = RequestMethod.GET)
  public Profile getProfile(
    @CookieValue("uid") Optional<String> uid,
    @RequestHeader("user") Optional<String> user,
    @RequestHeader("password") Optional<String> pass) {
    Profile profile = new Profile();
    profile.name = "bob";
    if (uid.isPresent() || user.isPresent() && pass.isPresent()) {
      profile.phone = "31415926535";
      return profile;
    } else {
      return profile;
    }
  }

  // curl dev:8180/api/x/100;name=apple;color=red/y/200;name=orange
  @RequestMapping(value = "/x/{x}/y/{y}")
  public Map coordinateProfile(
    @PathVariable int x, @MatrixVariable(pathVar = "x") Map<String, String> xCnf,
    @PathVariable int y, @MatrixVariable(pathVar = "y") Map<String, String> yCnf) {
    return new HashMap<Integer, Map>() {{
      put(x, xCnf);
      put(y, yCnf);
    }};
  }
}
