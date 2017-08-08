package example.api;

import java.util.*;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import commons.spring.RedisRememberMeService;
import commons.utils.*;
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
  public ApiResult listMemo(@AuthenticationPrincipal RedisRememberMeService.User user) {
    return memoManager.list(user.getId());
  }

  @ApiMethod(description = "add memo")
  @RequestMapping(value = "/memo", method = RequestMethod.POST)
  public ApiResult add(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiQueryParam(name = "memo", description = "memo")
    @RequestParam String memo) {
    return memoManager.add(user.getId(), memo);
  }

  @ApiMethod(description = "update a memo by memoId")
  @RequestMapping(value = "/memo/{memoId}", method = RequestMethod.PUT)
  public ApiResult update(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiPathParam(name = "memoId", description = "the memo's id", format = "digit")
    @PathVariable String memoId,
    @ApiQueryParam(name = "memo", description = "memo")
    @RequestParam String memo) {
    return memoManager.update(user.getId(), memoId, memo);
  }

  @ApiMethod(description = "delete one memo by memoId")
  @RequestMapping(value = "/memo/{memoId}", method = RequestMethod.DELETE)
  public ApiResult delete(
    @AuthenticationPrincipal RedisRememberMeService.User user,
    @ApiPathParam(name = "memoId", description = "the memo's id", format = "digit")
    @PathVariable String memoId) {
    return memoManager.delete(user.getId(), memoId);
  }
}
