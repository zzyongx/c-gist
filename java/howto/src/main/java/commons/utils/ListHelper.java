package commons.utils;

import java.util.List;
import java.util.ArrayList;

public class ListHelper {
  public static List<Object> make(Object ... varArgs) {
    List<Object> list = new ArrayList<>();

    for (int i = 0; i < varArgs.length; ++i) {
      list.add(varArgs[i]);
    }

    return list;
  }

  @SafeVarargs
  public static <T> List<T> makeExact(T ... varArgs) {
    List<T> list = new ArrayList<>();
    for (int i = 0; i < varArgs.length; ++i) {
      list.add(varArgs[i]);
    }
    return list;
  }
}
