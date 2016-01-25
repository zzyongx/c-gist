package example.api;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import example.model.*;
import example.manager.*;

@RestController
@RequestMapping("/api")
public class MemoController {
  @Autowired MemoManager memoManager;
  
  @RequestMapping(value = "/memo", method = RequestMethod.GET)
  public ApiResult listMemo() {
    return memoManager.list();
  }

  @RequestMapping(value = "/memo", method = RequestMethod.POST)
  public ApiResult add(@RequestParam String memo) {
    return memoManager.add(memo);
  }  

  @RequestMapping(value = "/memo/{memoId}", method = RequestMethod.PUT)
  public ApiResult update(@PathVariable String memoId, @RequestParam String memo) {
    return memoManager.update(memoId, memo);
  }

  @RequestMapping(value = "/memo/{memoId}", method = RequestMethod.DELETE)
  public ApiResult delete(@PathVariable String memoId) {
    return memoManager.delete(memoId);
  }
}
