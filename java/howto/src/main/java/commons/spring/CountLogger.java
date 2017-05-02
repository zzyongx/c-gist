package commons.spring;

import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;

public class CountLogger {
  private static final Logger logger = LoggerFactory.getLogger(CountLogger.class);

  @ManagedResource(objectName = "bean:name=counter")
  public static class Counter {
    private int count[] = {0, 0};
    private int minute[] = {0, 0};

    private static int getMinute() {
      return (int) (System.currentTimeMillis() / 60_000L);
    }

    private synchronized int add(int minute, int c) {
      int idx = minute % 2;
      if (this.minute[idx] == minute) {
        count[idx] += c;
      } else {
        this.minute[idx] = minute;
        count[idx] = c;
      }
      return count[idx];
    }

    private synchronized int getCount(int minute) {
      for (int i = 0; i < 2; ++i) {
        if (this.minute[i]+1 == minute) return count[i];
      }
      return 0;
    }

    public int add(int c) {
      return add(getMinute(), c);
    }

    @ManagedAttribute(description = "get counter")
    public int getCount() {
      return getCount(getMinute());
    }
  }

  private static CountLogger cc = new CountLogger();
  private CountLogger() {}

  private Map<String, Counter> beans = new HashMap<>();

  public static CountLogger register(String names) {
    if (names == null || names.isEmpty()) return cc;

    for (String name : names.split(",")) {
      cc.beans.put(name, new Counter());
    }
    return cc;
  }

  public static Map<String, Object> getBeans() {
    Map<String, Object> map = new HashMap<>();
    for (Map.Entry<String, Counter> entry : cc.beans.entrySet()) {
      map.put("counter." + entry.getKey(), entry.getValue());
    }
    return map;
  }

  public static void count(String name) {
    count(name, 1, null, null);
  }

  public static void count(String name, int count) {
    count(name, count, null, null);
  }

  public static void count(String name, String format, Object ... args) {
    count(name, 1, format, args);
  }

  public static void count(String name, int count, String format, Object ... args) {
    Counter counter = cc.beans.get(name);
    if (counter != null) counter.add(count);
    if (format != null) {
      logger.error(format, args);
    }
  }

  public static void debug(String format, Object ... args) {
    logger.debug(format, args);
  }

  public static void warn(String format, Object ... args) {
    logger.warn(format, args);
  }

  public static void error(String format, Object ... args) {
    logger.error(format, args);
  }
}
