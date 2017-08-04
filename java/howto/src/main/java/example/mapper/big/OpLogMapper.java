package example.mapper.big;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;

public interface OpLogMapper {
  class Sql {
    final static String TABLE = "oplog";
    public static final String INSERT = "INSERT INTO " + TABLE +
      " VALUES(null, #{uid}, #{action}, null)";
  }

  @Insert(Sql.INSERT)
  int add(@Param("uid") long uid, @Param("action") String action);
}
