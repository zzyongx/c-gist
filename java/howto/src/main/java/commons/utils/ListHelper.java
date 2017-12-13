package commons.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

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

  public static <T, E> List<T> sort(List<T> list, List<E> orders, Function<T,E> field) {
    Map<E, Integer> map = new HashMap<>();
    int i = 0;
    for (E e : orders) map.put(e, i++);

    list.sort((x, y) -> Integer.compare(map.get(field.apply(x)), map.get(field.apply(y))));
    return list;
  }

  public static <T, E> T[] fillSort(List<T> list, Class<T> clazz, List<E> orders, Function<T,E> field) {
    Map<E, T> map = new HashMap<>();
    for (T e : list) map.put(field.apply(e), e);

    @SuppressWarnings("unchecked")
    T[] array = (T[]) Array.newInstance(clazz, orders.size());

    int i = 0;
    for (E e : orders) array[i++] = map.get(e);
    return array;
  }

  public static <T, R> List<R> map(List<T> list, Function<T, R> field) {
    List<R> list2 = new ArrayList<>();
    for (T e : list) list2.add(field.apply(e));
    return list2;
  }
}
