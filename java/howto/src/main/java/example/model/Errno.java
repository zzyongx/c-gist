package example.model;

import org.jsondoc.core.annotation.*;
import commons.utils.BaseErrno;

@ApiObject(name = "Errno", description = "error code")
public class Errno extends BaseErrno {
  @ApiObjectField(description = "XXXX")
  public static final int YOU_CODE = -1;
}
