package commons.utils;

import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamHelper {
  public static <T> List<T> toList(Stream<T> stream) {
    return stream.collect(Collectors.toList());
  }

  public static <K, V, T> Map<K, V> toMap(
    Stream<T> stream, Function<T, ? extends K> keyMapper, Function<T, ? extends V> valueMapper) {
    return stream.collect(Collectors.toMap(keyMapper, valueMapper));
  }
}
