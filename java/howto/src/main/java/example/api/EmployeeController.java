package example.api;

import java.util.*;
import javax.validation.Valid;
import org.jsondoc.core.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import example.model.*;
import example.entity.*;
import example.dao.*;

@Api(name = "employee API",
     description = "demonstrate how to use Mybatis"
)
@ApiAuthToken
@RestController
@RequestMapping("/api")
public class EmployeeController {
  @Autowired EmployeeDao employeeDao;

  @ApiMethod(description = "Get all employees")
  @RequestMapping(value = "/employee", method = RequestMethod.GET)
  public ApiResult listEmployee() {
    List<Employee> employees = employeeDao.findAll();
    return new ApiResult<List>(employees);
  }

  @ApiMethod(description = "Add employee")
  @RequestMapping(value = "/employee", method = RequestMethod.POST,
                  consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                              MediaType.MULTIPART_FORM_DATA_VALUE}
  ) 
  public ApiResult add(
    @ApiBodyObject @RequestBody @Valid Employee employee, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ApiResult.bindingResult(bindingResult);
    }
    employeeDao.add(employee);
    return new ApiResult<Employee>(employee);
  }  

  @ApiMethod(description = "modify employee info by id")
  @RequestMapping(value = "/employee/{id}", method = RequestMethod.PUT,
                  consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                              MediaType.MULTIPART_FORM_DATA_VALUE}
  )
  public ApiResult update(
    @ApiPathParam(name = "id", description = "employee id")
    @PathVariable long id,
    @ApiBodyObject @RequestBody @Valid Employee employee, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ApiResult.bindingResult(bindingResult);
    }
    employee.setId(id);
    employeeDao.update(employee);
    return new ApiResult<Employee>(employee);
  }

  @ApiMethod(description = "delete employee by id")
  @RequestMapping(value = "/employee/{id}", method = RequestMethod.DELETE)
  public ApiResult delete(
    @ApiPathParam(name = "id", description = "employee id")
    @PathVariable long id) {
    employeeDao.delete(id);
    return ApiResult.ok();
  }
}
