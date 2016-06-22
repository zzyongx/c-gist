package example.api;

import javax.sql.DataSource;
import java.util.*;
import org.jsondoc.core.annotation.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.*;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import commons.mybatis.*;
import example.model.*;
import example.entity.*;
import example.mapper.*;
import example.config.*;

@Api(name = "employee API",
     description = "demonstrate how to use Mybatis"
)
@RestController
@RequestMapping("/api")
public class EmployeeDemoController {
  @Autowired @Qualifier("dbHorizontalPartition")
  ArrayList<EmployeeMapper> employeeMapperList;

  @Autowired @Qualifier("smartEmployeeMapper")
  EmployeeMapper smartEmployeeMapper;

  /* demonostrate database horizontal partition */
  @ApiMethod(description = "Employee.getFast: get employee by Id")
  @RequestMapping(value = "/employeeFast/{id}", method = RequestMethod.GET)
  public ApiResult getEmployeeFast(
    @ApiPathParam(name = "id", description = "employee id", format = "digit")
    @PathVariable long id) {
    int dbIdx = (int) (id % employeeMapperList.size());
    EmployeeMapper employeeMapper = employeeMapperList.get(dbIdx);
    Employee employee = employeeMapper.find(id);
    if (employee != null) return new ApiResult<Employee>(employee);
    else return ApiResult.notFound();
  }

  /* demonstrate smart database */
  @ApiMethod(description = "Employee.smart: ")
  @RequestMapping(value = "/employeeSmart", method = RequestMethod.GET)
  public ApiResult getEmployeeSmart() {
    List<Employee> employees = smartEmployeeMapper.findAll(10);
    for (Employee e : employees) {
      Employee employee = smartEmployeeMapper.find(e.getId());
      employee.setGender(Employee.Gender.MALE);
      smartEmployeeMapper.update(employee);
    }

    return new ApiResult<List>(SmartDataSource.ContextHolder.getDebug());
  }  

  /* demonstrate manual wired */
  @Autowired DataSource dataSource;

  @ApiMethod(description = "Employee.getManual: manual wired")
  @RequestMapping(value = "/employeeManual/{id}", method = RequestMethod.GET)
  public ApiResult getEmployeeManual(
    @ApiPathParam(name = "id", description = "employee id", format = "digit")
    @PathVariable long id) throws Exception {

    SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
    sqlSessionFactoryBean.setDataSource(dataSource);
    
    SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) sqlSessionFactoryBean.getObject();
    sqlSessionFactory.getConfiguration().addMapper(EmployeeMapper.class);
    sqlSessionFactory.getConfiguration().getTypeHandlerRegistry().register(
      Employee.Gender.class, new EnumValueTypeHandler<Employee.Gender>(Employee.Gender.class));

    SqlSessionTemplate sqlSession = new SqlSessionTemplate(sqlSessionFactory);
    EmployeeMapper employeeMapper = sqlSession.getMapper(EmployeeMapper.class);
    Employee employee = employeeMapper.find(id);
    if (employee != null) return new ApiResult<Employee>(employee);
    else return ApiResult.notFound();
  } 
}
