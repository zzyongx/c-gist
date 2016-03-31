package commons.mybatis;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.*;

public class EnumValueTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

  private Class<E> type;
  private Method   method;
  private final E[] enums;
  private final int[] values;

  public EnumValueTypeHandler(Class<E> type) {
    if (type == null) {
      throw new IllegalArgumentException("Type argument cannot be null");
    }
    this.type = type;
    this.enums = type.getEnumConstants();
    if (this.enums == null) {
      throw new IllegalArgumentException(
        type.getSimpleName() + " does not represent an enum value type.");
    }

    int i = 0; 
    try {
      this.method = type.getMethod("getValue");
      this.values = new int[enums.length];
      
      for (E e : this.enums) {
        this.values[i++] = (int) this.method.invoke(e);
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(
        type.getSimpleName() + " does not represent an enum value type.");
    }
  }

  public E getEnum(int value) {
    for (int i = 0; i < values.length; ++i) {
      if (values[i] == value) return enums[i];
    }
    return null;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
    int value;
    try {
      value = (int) this.method.invoke(parameter);
    } catch (Exception e) {
      throw new IllegalArgumentException(
        type.getSimpleName() + " does not represent an enum value type.");
    }
    
    ps.setInt(i, value);
  }

  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    int i = rs.getInt(columnName);
    if (rs.wasNull()) {
      return null;
    } else {
      try {
        return getEnum(i);
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }

  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    int i = rs.getInt(columnIndex);
    if (rs.wasNull()) {
      return null;
    } else {
      try {
        return getEnum(i);
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }

  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    int i = cs.getInt(columnIndex);
    if (cs.wasNull()) {
      return null;
    } else {
      try {
        return getEnum(i);
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }
  
}
