package example.api;

import javax.sql.DataSource;
import java.util.*;
import javax.validation.Valid;
import org.jsondoc.core.annotation.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.*;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import example.model.*;
import example.entity.*;
import example.mapper.*;
import example.config.EnumValueTypeHandler;

@Api(name = "employee API",
     description = "demonstrate how to use Mybatis"
)
@RestController
@RequestMapping("/api")
public class EmployeeController {
  @Autowired EmployeeMapper employeeMapper;

  @Autowired @Qualifier("dbHorizontalPartition")
  ArrayList<EmployeeMapper> employeeMapperList;

  @ApiMethod(description = "Employee.getAll: Get all employees")
  @RequestMapping(value = "/employees", method = RequestMethod.GET)
  public ApiResult listEmployee(
    @ApiQueryParam(name = "count", description = "limit returned employee count")
    @RequestParam(required = false) Optional<Integer> count) {
    List<Employee> employees = employeeMapper.findAll(count.orElse(100));
    return new ApiResult<List>(employees);
  }

  @ApiMethod(description = "Employee.getPage: get paged employees")
  @RequestMapping(value = "/employees/{pagerId}", method = RequestMethod.GET)
  public ApiResult listEmployee(
    @ApiPathParam(name = "pagerId", description = "employee id used by pager")
    @PathVariable long pagerId,
    @ApiQueryParam(name = "count", description = "limit return employee count")
    @RequestParam(required = false) Optional<Integer> count) {
    List<Employee> employees = employeeMapper.findPager(pagerId, count.orElse(100));
    return new ApiResult<List>(employees);
  }

  @ApiMethod(description = "Employee.getByName: get employees by name")
  @RequestMapping(value = "/employees/name/{name}", method = RequestMethod.GET)
  public ApiResult listEmployeeByName(
    @ApiPathParam(name = "name", description = "employee name")
    @PathVariable String name,
    @ApiQueryParam(name = "gender", description = "employee's gender")
    @RequestParam(required = false) Employee.Gender gender) {
    /* if mybatis return Map as Entity, the key is field name, and value is field value
     * the value type is Object, the JavaType and JdbcType's map is handled by TypeHandler.
     * When return Map, we have a problem. Because we don't know the target JavaType,
     * String or Integer may be used as Enum.
     */
    List<Map<String, Object>> employees = employeeMapper.findByName(name, gender);
    return new ApiResult<List>(employees);
  }

  @ApiMethod(description = "Employee.get: get employee by Id")
  @RequestMapping(value = "/employee/{id}", method = RequestMethod.GET)
  public ApiResult getEmployee(
    @ApiPathParam(name = "id", description = "employee id", format = "digit")
    @PathVariable long id) {
    Employee employee = employeeMapper.find(id);
    if (employee != null) return new ApiResult<Employee>(employee);
    else return ApiResult.notFound();
  }

  @ApiMethod(description = "Employee.add: Add employee")
  @RequestMapping(value = "/employee", method = RequestMethod.POST,
                  consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  ) 
  public ApiResult add(
    @ApiBodyObject @Valid Employee employee, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ApiResult.bindingResult(bindingResult);
    }
    employeeMapper.add(employee);
    return new ApiResult<Employee>(employee);
  }  

  @ApiMethod(description = "Employee.update: modify employee info by id")
  @RequestMapping(value = "/employee/{id}", method = RequestMethod.PUT,
                  consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  )
  public ApiResult update(
    @ApiPathParam(name = "id", description = "employee id")
    @PathVariable long id,
    @ApiBodyObject Employee employee, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ApiResult.bindingResult(bindingResult);
    }
    employee.setId(id);
    employeeMapper.update(employee);
    return new ApiResult<Employee>(employee);
  }

  @ApiMethod(description = "Employee.delete: delete employee by id")
  @RequestMapping(value = "/employee/{id}", method = RequestMethod.DELETE)
  public ApiResult delete(
    @ApiPathParam(name = "id", description = "employee id")
    @PathVariable long id) {
    employeeMapper.delete(id);
    return ApiResult.ok();
  }

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
