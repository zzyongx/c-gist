package commons.mybatis;

import java.sql.SQLException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.time.LocalDate;
import org.apache.ibatis.type.*;

@MappedTypes(LocalDate.class)
public class LocalDateTypeHandler implements TypeHandler<LocalDate> {
  @Override
  public void setParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
    throws SQLException {
    if (null != parameter) {
      ps.setDate(i, Date.valueOf(parameter));
    } else {
      ps.setDate(i, null);
    }
  }

  @Override
  public LocalDate getResult(ResultSet rs, String columnName) throws SQLException {
    Date ts = rs.getDate(columnName);
    if (null == ts) return null;
    return ts.toLocalDate();
  }

  @Override
  public LocalDate getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getDate(columnIndex).toLocalDate();
  }

  @Override
  public LocalDate getResult(ResultSet rs, int columnIndex) throws SQLException {
    Date ts = rs.getDate(columnIndex);
    if (null == ts) return null;
    return ts.toLocalDate();
  }
}
