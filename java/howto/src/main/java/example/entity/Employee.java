package example.entity;

import org.hibernate.validator.constraints.NotBlank;

public class Employee {
  long   id;
  
  @NotBlank(message = "name is required")
  String name;
  
  String department;
  String position;
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
