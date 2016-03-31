package commons.annotation;

import java.lang.annotation.*;

@Documented
@Target(value = { ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PartitionKey {
  public String value() default "";
}
