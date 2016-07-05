package commons.mybatis;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Paging extends HashMap<String, Object> {
  private String  table;
  private String  rowId;
  private String  where;
  private String  fields = "*";
  private int     count = 0;
  private boolean first;
  private boolean backward;

  public String toString() {
    return super.toString() + "\ntable: " + table + "\nrowId:" + rowId + "\nwhere:" + where;
  }

  public String sql() {
    if (where == null && first) {
      return String.format("SELECT %s FROM %s ORDER BY %s DESC LIMIT %d",
                           rowId, table, rowId, count);
      
    } else if (where == null && backward) {
      return String.format("SELECT %s FROM %s WHERE %s < #{%s} ORDER BY %s DESC LIMIT %d",
                           rowId, table, rowId, rowId, rowId, count);
      
    } else if (where == null && !backward) {
      return String.format("SELECT %s FROM %s WHERE %s > #{%s} ORDER BY %s ASC LIMIT %d",
                           rowId, table, rowId, rowId, rowId, count);
      
    } else if (where != null && first) {
      return String.format("SELECT %s FROM %s WHERE %s ORDER BY %s DESC LIMIT %d",
                           rowId, table, where, rowId, count);
      
    } else if (where != null && backward) {
      return String.format("SELECT %s FROM %s WHERE (%s) AND %s < #{%s} ORDER BY %s DESC LIMIT %d",
                           rowId, table, where, rowId, rowId, rowId, count);
      
    } else if (where != null && !backward) {
      return String.format("SELECT %s FROM %s WHERE (%s) AND %s > #{%s} ORDER BY %s ASC LIMIT %d",
                           rowId, table, where, rowId, rowId, rowId, count);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public Paging setTable(String table) {
    this.table = table;
    return this;
  }

  public Paging setRowId(String rowId) {
    this.rowId = rowId;
    return this;
  }

  public Paging setCount(int pages, int count) {
    this.count = pages * count;
    return this;
  }

  public Paging setWhere(String where) {
    this.where = where;
    return this;
  }

  public Paging andWhere(String where) {
    if (this.where == null) {
      this.where = where;
    } else {
      this.where = this.where + " AND " + where;
    }
    return this;
  }

  public <T> Paging andInCluster(String field, List<T> list, Class<T> clazz) {
    if (list == null || list.isEmpty()) return this;
    
    try {
      Method method = clazz.getMethod("getValue");
      
      StringBuilder builder = new StringBuilder();
      builder.append(field).append(" in (");
      
      String separator = "";
      for (T t : list) {
        builder.append(separator).append(method.invoke(t));
        separator = ",";
      }
      builder.append(')');
      return andWhere(builder.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Paging setFields(String fields) {
    this.fields = fields;
    return this;
  }

  public Paging setFirst(boolean first) {
    this.first = first;
    return this;
  }

  public Paging setBackward(boolean backward) {
    this.backward = backward;
    return this;
  }

  public Paging setParams(Object ... varArgs) {
    for (int i = 0; i < varArgs.length; i += 2) {
      put((String) varArgs[i], varArgs[i+1]);
    }
    return this;
  }

  public static String first(Paging paging) {
    paging.setFirst(true);
    return paging.sql();
  }
  
  public static String forward(Paging paging) {
    paging.setFirst(false);
    paging.setBackward(false);
    return paging.sql();
  }

  public static String backward(Paging paging) {
    paging.setFirst(false);
    paging.setBackward(true);
    return paging.sql();
  }

  public String listSql() {
    if (where == null && containsKey(rowId)) {
      return String.format("SELECT %s FROM %s WHERE %s <= #{%s} ORDER BY %s DESC LIMIT %d",
                           fields, table, rowId, rowId, rowId, count);
      
    } else if (where == null && !containsKey(rowId)) {
      return String.format("SELECT %s FROM %s ORDER BY %s DESC limit %d",
                           fields, table, rowId, count);

    } else if (where != null && containsKey(rowId)) {
      return String.format("SELECT %s FROM %s WHERE (%s) AND %s <= #{%s} ORDER BY %s DESC LIMIT %d",
                           fields, table, where, rowId, rowId, rowId, count);

    } else if (where != null && !containsKey(rowId)) {
      return String.format("SELECT %s FROM %s WHERE %s ORDER BY %s DESC LIMIT %d",
                           fields, table, where, rowId, count);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static String list(Paging paging) {
    return paging.listSql();
  }

  public static class Page<T> {
    public T min;
    public T max;
    
    public Page(T min, T max) {
      this.min = min;
      this.max = max;
    }
  }

  public static <T extends Comparable> List<Page<T>> pages(List<T> list, int count) {
    List<Page<T>> pageList = new ArrayList<>();
    if (list.size() >= 2) {
      @SuppressWarnings("unchecked")
      int r = list.get(0).compareTo(list.get(1));
      if (r < 0) Collections.reverse(list);
    }
    
    for (int i = 0; i < list.size(); i += count) {
      T max = list.get(i);
      T min;
      if (i + count < list.size()) {
        min = list.get(i + count - 1);
      } else {
        min = list.get(list.size() - 1);
      }
      pageList.add(new Page<T>(min, max));
    }
    
    return pageList;
  }
}
