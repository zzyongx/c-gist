package example.dao;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import example.entity.Employee;

public interface EmployeeDao {
  class Sql {
    final static String TABLE = "employee";
    final static String SELECT_ALL = "SELECT * FROM " + TABLE;
    final static String DELETE = "DELETE FROM " + TABLE + " WHERE id = #{id}";

    public static String insert(Employee employee) {
      SQL sql = new SQL()
        .INSERT_INTO(TABLE)
        .VALUES("name", "#{name}")
        .VALUES("phone", "#{phone}");
      if (employee.getDepartment() != null) {
        sql.VALUES("department", "#{department}");
      }
      if (employee.getPosition() != null) {
        sql.VALUES("position", "#{position}");
      }
      return sql.toString();
    }

    public static String update(Employee employee) {
      SQL sql = new SQL().UPDATE(TABLE);
      if (employee.getName() != null) {
        sql.SET("name = #{name}");
      }
      if (employee.getDepartment() != null) {
        sql.SET("department = #{department}");
      }
      if (employee.getPosition() != null) {
        sql.SET("position = #{position}");
      }
      return sql.SET("phone = #{phone}")
        .WHERE("id = #{id}").toString();        
    }
  }

  @Select(Sql.SELECT_ALL)
  List<Employee> findAll();

  @InsertProvider(type = Sql.class, method = "insert")
  @Options(useGeneratedKeys=true, keyProperty = "id")
  int add(Employee employee);

  @UpdateProvider(type = Sql.class, method = "update")
  int update(Employee employee);

  @Delete(Sql.DELETE)
  int delete(long id);
}
