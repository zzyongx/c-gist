package example.api;

import java.util.*;
import javax.validation.Valid;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;
import example.mapper.big.*;
import commons.utils.*;

/* Warning: you shouldn't use Mapper in Controller, add Manager layer */

@Api(name = "opLog API",
     description = "demonstrate how to use MultiDb"
)
@RestController
@RequestMapping("/api")
public class OpLogController {
  @Autowired OpLogMapper    opLogMapper;

  @ApiMethod(description = "add op log")
  @RequestMapping(value = "/oplog", method = RequestMethod.POST)
  public ApiResult add(
    @ApiQueryParam(name = "id", description = "user id")
    @RequestParam int id,
    @ApiQueryParam(name = "log", description = "log")
    @RequestParam String log) {
    opLogMapper.add(id, log);
    return ApiResult.ok();
  }
}
