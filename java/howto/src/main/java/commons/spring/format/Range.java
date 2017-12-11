package commons.spring.format;

import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class Range<T> {
  public T min;
  public T max;

  public abstract boolean contains(T t);

  public static class IntType extends Range<Integer> {
    @Override
    public boolean contains(Integer t) {
      return t != null && t >= min && (max == null || t <= max);
    }
  }

  public static class LongType extends Range<Long> {
    @Override
    public boolean contains(Long t) {
      return t != null && t >= min && (max == null || t <= max);
    }
  }

  public static class StringType extends Range<String> {
    @Override
    public boolean contains(String t) {
      return t != null && min.compareTo(t) <= 0 && (max == null || max.compareTo(t) >= 0);
    }
  }

  public static class LocalDateType extends Range<LocalDate> {
    @Override
    public boolean contains(LocalDate t) {
      return t != null && (min.isBefore(t) || min.isEqual(t)) &&
        (max == null || (t.isBefore(max) || t.isEqual(max)));
    }
  }

  public static class LocalDateTimeType extends Range<LocalDateTime> {
    @Override
    public boolean contains(LocalDateTime t) {
      return t != null && (min.isBefore(t) || min.isEqual(t)) &&
        (max == null || (t.isBefore(max) || t.isEqual(max)));
    }
  }

  public static class DoubleType extends Range<Double> {
    @Override
    public boolean contains(Double t) {
      return t != null && t >= min && (max == null || t <= max);
    }
  }
}
