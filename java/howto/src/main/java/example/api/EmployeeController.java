package example.api;

import java.util.*;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import example.model.*;
import example.entity.*;
import example.dao.*;

@RestController
@RequestMapping("/api")
public class EmployeeController {
  @Autowired EmployeeDao employeeDao;
  
  @RequestMapping(value = "/employee", method = RequestMethod.GET)
  public ApiResult listMemo() {
    List<Employee> employees = employeeDao.findAll();
    return new ApiResult<List>(employees);
  }

  @RequestMapping(value = "/employee", method = RequestMethod.POST)
  public ApiResult add(@Valid Employee employee, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ApiResult.bindingResult(bindingResult);
    }
    employeeDao.add(employee);
    return new ApiResult<Employee>(employee);
  }  

  @RequestMapping(value = "/employee/{id}", method = RequestMethod.PUT)
  public ApiResult update(@PathVariable long id, @Valid Employee employee,
                          BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ApiResult.bindingResult(bindingResult);
    }
    employee.setId(id);
    employeeDao.update(employee);
    return new ApiResult<Employee>(employee);
  }

  @RequestMapping(value = "/employee/{id}", method = RequestMethod.DELETE)
  public ApiResult delete(@PathVariable long id) {
    employeeDao.delete(id);
    return ApiResult.ok();
  }
}
