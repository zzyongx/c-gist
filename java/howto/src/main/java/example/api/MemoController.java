package example.api;

import java.util.*;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import example.model.*;
import example.manager.*;

@Api(
  name = "memo API",
  description = "Read/Write/Update/Delete the memo. Demonstrate how JedisPool autowired worked"
)
@RestController
@RequestMapping("/api")
public class MemoController {
  @Autowired MemoManager memoManager;

  @ApiMethod(description = "get all memo")
  @RequestMapping(value = "/memo", method = RequestMethod.GET)
  public ApiResult listMemo() {
    return memoManager.list();
  }

  @ApiMethod(description = "add memo")
  @RequestMapping(value = "/memo", method = RequestMethod.POST,
                  consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                              MediaType.MULTIPART_FORM_DATA_VALUE}
  )
  public ApiResult add(
    @ApiQueryParam(name = "memo", description = "memo")
    @RequestParam String memo) {
    return memoManager.add(memo);
  }  

  @ApiMethod(description = "update a memo by memoId")
  @RequestMapping(value = "/memo/{memoId}", method = RequestMethod.PUT,
                  consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                              MediaType.MULTIPART_FORM_DATA_VALUE}                  
  )
  public ApiResult update(
    @ApiPathParam(name = "memoId", description = "the memo's id", format = "digit")
    @PathVariable String memoId,
    @ApiQueryParam(name = "memo", description = "memo")
    @RequestParam String memo) {
    return memoManager.update(memoId, memo);
  }

  @ApiMethod(description = "delete one memo by memoId")
  @RequestMapping(value = "/memo/{memoId}", method = RequestMethod.DELETE)
  public ApiResult delete(
    @ApiPathParam(name = "memoId", description = "the memo's id", format = "digit")
    @PathVariable String memoId) {
    return memoManager.delete(memoId);
  }
}
