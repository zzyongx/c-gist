package commons.spring.format;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.util.StringUtils;

public class RangeEditor<T> extends PropertyEditorSupport {
  private Class<T> type;

  public RangeEditor(Class<T> type) {
    this.type = type;
  }

  private Range stringToRange(Class<T> rtype, String min, String max) {
    if (rtype == Integer.class) {
      Range.IntType range = new Range.IntType();
      range.min = Integer.valueOf(min);
      if (max != null) range.max = Integer.valueOf(max);
      return range;
    } else if (rtype == Long.class) {
      Range.LongType range = new Range.LongType();
      range.min = Long.valueOf(min);
      if (max != null) range.max = Long.valueOf(max);
      return range;
    } else if (rtype == String.class) {
      Range.StringType range = new Range.StringType();
      range.min = min;
      range.max = max;
      return range;
    } else if (rtype == LocalDate.class) {
      Range.LocalDateType range = new Range.LocalDateType();
      range.min = LocalDate.parse(min);
      if (max != null) range.max = LocalDate.parse(max);
      return range;
    } else if (rtype == LocalDateTime.class) {
      Range.LocalDateTimeType range = new Range.LocalDateTimeType();
      range.min = LocalDateTime.parse(min);
      if (max != null) range.max = LocalDateTime.parse(max);
      return range;
    } else if (rtype == Double.class) {
      Range.DoubleType range = new Range.DoubleType();
      range.min = Double.valueOf(min);
      if (max != null) range.max = Double.valueOf(max);
      return range;
    } else {
      throw new IllegalArgumentException("unsupported return type " + rtype);
    }
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      String ch = "-";
      if (type == LocalDate.class || type == LocalDateTime.class || text.indexOf(';') > 0) ch = ";";

      String parts[] = text.split(ch, 2);
      setValue(stringToRange(type, parts[0], parts.length == 2 ? parts[1] : null));
    } else {
      setValue(null);
    }
  }
}
