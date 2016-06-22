package example.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProjectInfo {
  public static final String PKG_PREFIX    = "example";
  public static final String API_PKG       = "example.api";
  public static final String MAPPER_PKG    = "example.mapper";
  public static final List<String> DOC_PKG = Collections.unmodifiableList(
    Arrays.asList("example.api", "example.entity", "example.model"));
}
