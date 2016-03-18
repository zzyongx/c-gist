package example.entity;

import org.hibernate.validator.constraints.NotBlank;
import org.jsondoc.core.annotation.*;

@ApiObject(name = "Employee", description = "Employee info")
public class Employee {
  @ApiObjectField(description = "employee id")
  long   id;

  @ApiObjectField(description = "employee name", required = true)
  @NotBlank(message = "name is required")
  String name;

  @ApiObjectField(description = "department name")
  String department;
  @ApiObjectField(description = "title and rank")
  String position;
  @ApiObjectField(description = "phone number")
  int    phone      = -1;

  public void setId(long id) {
    this.id = id;
  }
  public long getId() {
    return this.id;
  }

  public void setName(String name) {
    this.name = name;
  }
  public String getName() {
    return this.name;
  }

  public void setDepartment(String department) {
    this.department = department;
  }
  public String getDepartment() {
    return this.department;
  }

  public void setPosition(String position) {
    this.position = position;
  }
  public String getPosition() {
    return this.position;
  }

  public void setPhone(int phone) {
    this.phone = phone;
  }
  public int getPhone() {
    return this.phone;
  }    
}
