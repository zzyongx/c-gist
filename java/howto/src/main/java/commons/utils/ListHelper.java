package commons.utils;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

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

  public static <T> List<T> distinct(List<T> list, Comparator<T> comparator) {
    if (list == null || list.size() <= 1) return list;

    Set<T> set = new TreeSet<>(comparator);
    set.addAll(list);
    return new ArrayList<>(set);
  }
}
