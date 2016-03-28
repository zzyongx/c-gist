package example.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import example.entity.Employee;

public interface EmployeeMapper {
  class Sql {
    final static String TABLE = "employee";
    final static String SELECT = "SELECT * FROM " + TABLE + " WHERE id = #{id}";
    final static String SELECT_ALL = "SELECT * FROM " + TABLE + " ORDER BY id DESC LIMIT #{limit}";
    final static String SELECT_PAGE = "SELECT * FROM " + TABLE +
      " WHERE id < #{id} ORDER BY id LIMIT #{limit}";
    final static String DELETE = "DELETE FROM " + TABLE + " WHERE id = #{id}";

    public static String selectByName(Map<String, Object> param) {
      SQL sql = new SQL().SELECT("*").FROM(TABLE)
        .WHERE("name = #{name}");
      
      Employee.Gender gender = (Employee.Gender) param.get("gender");
      if (gender != null) {
        //sql.AND().WHERE("gender = #{gender,typeHandler=example.config.EnumValueTypeHandler}");
        sql.AND().WHERE("gender = #{gender}");
      }
      
      return sql.toString();
    }

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
      if (employee.getGender() != null) {
        sql.VALUES("gender", "#{gender}");
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
      if (employee.getGender() != null) {
        sql.SET("gender = #{gender}");
      }
      if (employee.getPhone() != Integer.MIN_VALUE) {
        sql.SET("phone = #{phone}");
      }
      return sql.WHERE("id = #{id}").toString();        
    }
  }

  @Select(Sql.SELECT_ALL)
  List<Employee> findAll(long limit);

  @Select(Sql.SELECT_PAGE)
  List<Employee> findPager(@Param("id") long id, @Param("limit") long limit);

  @SelectProvider(type = Sql.class, method = "selectByName")
  List<Map<String, Object>> findByName(@Param("name") String name,
                                       @Param("gender") Employee.Gender gender);

  @Select(Sql.SELECT)
  Employee find(@Param("id") long id);

  @InsertProvider(type = Sql.class, method = "insert")
  @Options(useGeneratedKeys=true, keyProperty = "id")
  int add(Employee employee);

  @UpdateProvider(type = Sql.class, method = "update")
  int update(Employee employee);

  @Delete(Sql.DELETE)
  int delete(long id);
}
