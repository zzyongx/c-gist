package example.api;

import java.sql.*;
import java.util.*;
import org.jsondoc.core.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static org.springframework.util.StringUtils.*;
import example.model.*;

@Api(name = "CodeAutoGen API",
     description = "auto generate code. MIN_VALUE and null as property missed."
)
@RestController
@RequestMapping("/api/code")
public class CodeAutoGen {
  public static class EntitySource {
    public String dbHostPort;
    public String dbUser;
    public String dbPassword;
    public String table;
    public String className;
    public String packagePrefix;
  }

  public static class FieldDesc {
    public String  name;
    public String  type;
    public boolean isAutoIncrement;
    public boolean isUniq;
  }

  public static class EntityDesc {
    public boolean hasDateTimeType;
    public boolean hasBigDecimalType;
    public boolean isPrimaryKeyAutoIncrement;
    public String autoIncrementKey;
    public List<FieldDesc> uniqKeys;
    
    public List<FieldDesc> fields;
    public void prepare() {
      hasDateTimeType = false;
      hasBigDecimalType = false;
      uniqKeys = new ArrayList<FieldDesc>();
      isPrimaryKeyAutoIncrement = false;
      
      for (FieldDesc field : fields) {
        if (field.isUniq) uniqKeys.add(field);
        if (field.isAutoIncrement) {
          isPrimaryKeyAutoIncrement = true;
          autoIncrementKey = field.name;
        }
        if (field.type.equals("LocalDateTime") || field.type.equals("LocalDate")) {
          hasDateTimeType = true;
        } else if (field.type.equals("BigDecimal")) {
          hasBigDecimalType = true;
        }
      }
    }
  }

  FieldDesc rsToFieldDesc(ResultSet rs) {
    try {
      FieldDesc field = new FieldDesc();
      field.name = rs.getString("Field");
            
      String type = rs.getString("Type");
      if (type.startsWith("bigint")) {
        field.type = "long";
      } else if (type.contains("int")) {
        field.type = "int";
      } else if (type.contains("char") || type.contains("text")) {
        field.type = "String";
      } else if (type.equals("datetime") || type.equals("timestamp")) {
        field.type = "LocalDateTime";
      } else if (type.equals("date")) {
        field.type = "LocalDate";
      } else if (type.startsWith("decimal")) {
        field.type = "BigDecimal";
      }

      field.isAutoIncrement = false;
      field.isUniq = false;            
            
      String key = rs.getString("Key");
      String extra = rs.getString("Extra");
      if (key.equals("PRI")) {
        field.isUniq = true;
        if (extra.equals("auto_increment")) {
          field.isAutoIncrement = true;
        }
      } else if (key.equals("UNI")) {
        field.isUniq = true;
      }
      return field;
    } catch (Exception e) {
      throw new Errno.InternalErrorException(e);
    }
  }

  EntityDesc getEntityDesc(EntitySource source) {
    SingleConnectionDataSource c = new SingleConnectionDataSource(
      "jdbc:mysql://" + source.dbHostPort + "/?characterEncoding=utf-8",
      source.dbUser, source.dbPassword, false);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(c);
    try {
      EntityDesc entityDesc = new EntityDesc();
      
      entityDesc.fields = jdbcTemplate.query(
        "desc " + source.table,
        new RowMapper<FieldDesc>() {
          public FieldDesc mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rsToFieldDesc(rs);
          }});
      
      entityDesc.prepare();  
      return entityDesc;
    } catch (Exception e) {
      throw new Errno.InternalErrorException(e);
    } finally {
      c.destroy();
    }
  }

  String genDaoClassName(String className) {
    return className + "Dao";
  }

  StringBuilder genDaoSelectSql(EntityDesc entityDesc) {
    StringBuilder sb = new StringBuilder();
    
    for (FieldDesc field : entityDesc.uniqKeys) {
      if (field.isAutoIncrement) {
        sb.append("    final static String SELECT = \"SELECT * FROM \" + TABLE + \" ORDER BY ")
          .append(field.name).append(" DESC LIMIT 100\";\n");
        sb.append("    final static String SELECT_PAGE = \"SELECT * FROM \" + TABLE + \" WHERE ")
          .append(field.name).append(" < #{").append(field.name).append("} ORDER BY ")
          .append(field.name).append(" DESC LIMIT 100\";\n\n");
      }

      sb.append("    final static String SELECT_BY_").append(field.name.toUpperCase())
        .append(" = \"SELECT * FROM \" + TABLE + \" WHERE ")
        .append(field.name).append(" = #{").append(field.name).append("}\";\n");
    }
    return sb;
  }
  
  StringBuilder genDaoSelectFun(EntityDesc entityDesc, String entityClassName) {
    StringBuilder sb = new StringBuilder();
    
    for (FieldDesc field : entityDesc.uniqKeys) {
      if (field.isAutoIncrement) {
        sb.append("  @Select(Sql.SELECT)\n");
        sb.append("  List<").append(entityClassName).append("> find();\n\n");
        
        sb.append("  @Select(Sql.SELECT_PAGE)\n");
        sb.append("  List<").append(entityClassName).append("> findPage(")
          .append(field.type).append( " id);\n\n");
      }

      sb.append("  @Select(Sql.SELECT_BY_").append(field.name.toUpperCase()).append(")\n");
      sb.append("  ").append(entityClassName).append(" findBy").append(capitalize(field.name))
        .append("(").append(field.type).append(" ").append(field.name).append(");\n");
    }
    return sb;
  }

  StringBuilder genDaoInsertSql(EntityDesc entityDesc, String entityClassName) {
    StringBuilder sb = new StringBuilder();
    
    sb.append("    public static String insert(").append(entityClassName).append(" entity) {\n");
    sb.append("      SQL sql = new SQL().INSERT_INTO(TABLE);\n");
    for (FieldDesc field : entityDesc.fields) {
      String capName = capitalize(field.name);
      String name = field.name;
      if (field.type.equals("long")) {
        sb.append("      if (entity.get").append(capName).append("() != Long.MIN_VALUE) {\n");
      } else if (field.type.equals("int")) {
        sb.append("      if (entity.get").append(capName).append("() != Integer.MIN_VALUE) {\n");
      } else {
        sb.append("      if (entity.get").append(capName).append("() != null) {\n");
      }
      sb.append("        sql.VALUES(\"").append(name).append("\", \"#{")
        .append(name).append("}\");\n");
      sb.append("      }\n\n");
    }
    sb.append("      return sql.toString();\n");
    sb.append("    }\n");

    return sb;
  }

  StringBuilder genDaoInsertFun(EntityDesc entityDesc, String entityClassName) {
    StringBuilder sb = new StringBuilder();
    
    sb.append("  @InsertProvider(type = Sql.class, method = \"insert\")\n");    
    if (entityDesc.isPrimaryKeyAutoIncrement) {
      sb.append("  @Options(useGeneratedKeys=true, keyProperty = \"")
        .append(entityDesc.autoIncrementKey).append("\")\n");
    }
    sb.append("  int insert(").append(entityClassName).append(" entity);\n");

    return sb;
  }

  StringBuilder genDaoUpdateSql(EntityDesc entityDesc, String entityClassName) {
    StringBuilder sb = new StringBuilder();

    for (FieldDesc field : entityDesc.uniqKeys) {
      String methodName = "updateBy" + capitalize(field.name);
      sb.append("    public static String ").append(methodName).append("(")
        .append(entityClassName).append(" entity) {\n");
      sb.append("      SQL sql = new SQL().UPDATE(TABLE);\n\n");
      
      for (FieldDesc f : entityDesc.fields) {
        if (f.name.equals(field.name)) continue;
        String capName = capitalize(f.name);
        
        if (f.type.equals("long")) {
          sb.append("      if (entity.get").append(capName).append("() != Long.MIN_VALUE) {\n");
        } else if (f.type.equals("int")) {
          sb.append("      if (entity.get").append(capName).append("() != Integer.MIN_VALUE) {\n");
        } else {
          sb.append("      if (entity.get").append(capName).append("() != null) {\n");
        }
        sb.append("        sql.SET(\"").append(f.name)
          .append(" = #{").append(f.name).append("}\");\n");
        sb.append("      }\n\n");
      }

      sb.append("      sql.WHERE(\"").append(field.name).append(" = #{").append(field.name)
        .append("}\");\n");
      sb.append("      return sql.toString();\n");
      sb.append("    }\n\n");
    }
    
    return sb;
  }

  StringBuilder genDaoUpdateFun(EntityDesc entityDesc, String entityClassName) {
    StringBuilder sb = new StringBuilder();

    for (FieldDesc field : entityDesc.uniqKeys) {
      String methodName = "updateBy" + capitalize(field.name);
      sb.append("  @UpdateProvider(type = Sql.class, method = \"")
        .append(methodName).append("\")\n");
      sb.append("  int ").append(methodName).append("(")
        .append(entityClassName).append(" entity);\n");
    }

    return sb;
  }  

  String genDaoCode(EntitySource source, EntityDesc entityDesc) {
    String daoClassName = genDaoClassName(source.className);
    String entityClassName = genEntityClassName(source.className);
    String entityFullClassName = genEntityFullClassName(source.packagePrefix, source.className);
      
    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(source.packagePrefix).append(".dao;").append("\n\n");

    sb.append("import java.util.*;").append("\n");
    sb.append("import org.apache.ibatis.annotations.*;").append("\n");
    sb.append("import org.apache.ibatis.jdbc.SQL;").append("\n");
    sb.append("import ").append(entityFullClassName).append(";\n");
    sb.append("\n");

    sb.append("public interface ").append(daoClassName).append(" {\n");
    sb.append("  class Sql {\n");
    sb.append("    final static String TABLE = \"").append(source.className).append("\";\n\n");
    
    sb.append(genDaoSelectSql(entityDesc));
    sb.append("\n");
    sb.append(genDaoInsertSql(entityDesc, entityClassName));
    sb.append("\n");
    sb.append(genDaoUpdateSql(entityDesc, entityClassName));
    sb.append("\n");

    sb.append("  }\n\n");

    sb.append(genDaoSelectFun(entityDesc, entityClassName));
    sb.append("\n");
    sb.append(genDaoInsertFun(entityDesc, entityClassName));
    sb.append("\n");
    sb.append(genDaoUpdateFun(entityDesc, entityClassName));
    sb.append("\n");

    sb.append("}\n");

    return sb.toString();
  }

  String genEntityClassName(String className) {
    return className;
  }
  
  String genEntityFullClassName(String packagePrefix, String className) {
    return packagePrefix + ".entity." + className;
  }

  String genEntityCode(EntitySource source, EntityDesc entityDesc) {
    String entityClassName = genEntityClassName(source.className);

    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(source.packagePrefix).append(".entity;\n\n");

    if (entityDesc.hasBigDecimalType) sb.append("import java.math.BigDecimal;\n");
    if (entityDesc.hasDateTimeType) sb.append("import java.time.*;\n");
    sb.append("import org.jsondoc.core.annotation.*;\n");
    sb.append("\n");

    sb.append("@ApiObject(name = \"").append(entityClassName).append("\", description = \"")
      .append(entityClassName).append("\")\n");
    sb.append("public class ").append(entityClassName).append(" {\n");

    for (FieldDesc field : entityDesc.fields) {
      sb.append("  @ApiObjectField(description = \"").append(field.name).append("\")\n");
      String defaultValue = "null";
      if (field.type.equals("long")) {
        defaultValue = "Long.MIN_VALUE";
      } else if (field.type.equals("int")) {
        defaultValue = "Integer.MIN_VALUE";
      }
      
      sb.append("  ").append(field.type).append(" ").append(field.name)
        .append(" = ").append(defaultValue).append(";\n\n");
    }
    
    for (FieldDesc field : entityDesc.fields) {
      sb.append("  public void set").append(capitalize(field.name))
        .append("(").append(field.type).append(" ").append(field.name).append(") {\n");
      sb.append("    this.").append(field.name).append(" = ").append(field.name).append(";\n");
      sb.append("  }\n");

      sb.append("  public ").append(field.type).append(" get").append(capitalize(field.name))
        .append("() {\n");
      sb.append("    return ").append(field.name).append(";\n");
      sb.append("  }\n\n");
    }

    sb.append("}\n");
    return sb.toString();
  }

  String genControllerClassName(String className) {
    return className + "Controller";
  }

  StringBuilder genControllerSelectFun(EntitySource source, EntityDesc entityDesc) {
    String managerVariableName = uncapitalize(genManagerClassName(source.className));
    String entityName = uncapitalize(source.className);
    StringBuilder sb = new StringBuilder();

    for (FieldDesc field : entityDesc.uniqKeys) {
      if (field.isAutoIncrement) {
        sb.append("  @ApiMethod(description = \"select last items\")\n");
        sb.append("  @RequestMapping(value = \"/").append(entityName)
          .append("s\", method = RequestMethod.GET)\n");
        sb.append("  public ApiResult find() {\n");
        sb.append("    return ").append(managerVariableName).append(".find();\n");
        sb.append("  }\n\n");

        sb.append("  @ApiMethod(description = \"select older items\")\n");
        sb.append("  @RequestMapping(value = \"/").append(entityName)
          .append("s/{pager}\", method = RequestMethod.GET)\n");
        sb.append("  public ApiResult findPage(\n");
        sb.append("    @ApiPathParam(name = \"pager\", description = \"pager\")\n");
        sb.append("    @PathVariable long pager) {\n");
        sb.append("    return ").append(managerVariableName).append(".find(pager);\n");
        sb.append("  }\n\n");
      }

      sb.append("  @ApiMethod(description = \"select by ").append(field.name).append("\")\n");
      sb.append("  @RequestMapping(value = \"/").append(entityName)
        .append("s/").append(field.name).append("/{").append(field.name)
        .append("}\", method = RequestMethod.GET)\n");
      sb.append("  public ApiResult findBy").append(capitalize(field.name)).append("(\n");
      sb.append("    @ApiPathParam(name = \"").append(field.name)
        .append("\", description = \"").append(field.name).append("\")\n");
      sb.append("    @PathVariable ").append(field.type).append(" ").append(field.name)
        .append(") {\n");
      sb.append("    return ").append(managerVariableName).append(".findBy")
        .append(capitalize(field.name)).append("(").append(field.name).append(");\n");
      sb.append("  }\n\n");
    }

    return sb;
  }

  StringBuilder genControllerInsertFun(EntitySource source, EntityDesc entityDesc) {
    String managerVariableName = uncapitalize(genManagerClassName(source.className));    
    StringBuilder sb = new StringBuilder();

    sb.append("  @ApiMethod(description = \"add ").append(source.className).append("\")\n");
    sb.append("  @RequestMapping(value = \"/").append(uncapitalize(source.className))
      .append("/\", method = RequestMethod.POST,\n")
      .append("                  consumes = {\"application/x-www-form-urlencoded\",\n")
      .append("                              \"multipart/form-data\"})\n");
    sb.append("  public ApiResult add(\n")
      .append("    @ApiBodyObject @RequestBody \n")
      .append("    @Valid ")
      .append(genEntityClassName(source.className))
      .append(" entity, BindingResult bindingResult) {\n");
    sb.append("    if (bindingResult.hasErrors()) {\n");
    sb.append("      return ApiResult.bindingResult(bindingResult);\n");
    sb.append("    }\n\n");
    sb.append("    return ").append(managerVariableName).append(".add(entity);\n");
    sb.append("  }\n");
    
    return sb;
  }

  StringBuilder genControllerUpdateFun(EntitySource source, EntityDesc entityDesc) {
    String managerVariableName = uncapitalize(genManagerClassName(source.className));
    String entityClassName = genEntityClassName(source.className);
    StringBuilder sb = new StringBuilder();

    for (FieldDesc field : entityDesc.uniqKeys) {
      String methodName = "updateBy" + capitalize(field.name);
      sb.append("  @ApiMethod(description = \"").append(methodName).append("\")\n");
      sb.append("  @RequestMapping(value = \"/").append(uncapitalize(source.className))
        .append("/").append(field.name).append("/{").append(field.name)
        .append("}\", method = RequestMethod.PUT,\n")
        .append("                  consumes = {\"application/x-www-form-urlencoded\",\n")
        .append("                              \"multipart/form-data\"})\n");
      sb.append("  public ApiResult ").append(methodName).append("(\n");

      for (FieldDesc f : entityDesc.fields) {
        if (f.name.equals(field.name)) continue;
        sb.append("    @ApiQueryParam(name = \"").append(f.name)
          .append("\", description = \"").append(f.name).append("\")\n");
        sb.append("    @RequestParam(required = false) ");
        if (f.type.equals("long")) sb.append("Optional<Long>");
        else if (f.type.equals("int")) sb.append("Optional<Integer>");
        else sb.append("Optional<").append(f.type).append(">");
        sb.append(" ").append(f.name).append(",\n\n");
      }

      sb.append("    @ApiPathParam(name = \"").append(field.name)
        .append("\", description = \"").append(field.name).append("\")\n");
      sb.append("    @PathVariable ").append(field.type)
        .append(" ").append(field.name).append(") {\n\n");

      sb.append("    ").append(entityClassName).append(" entity = new ")
        .append(entityClassName).append("();\n");
      for (FieldDesc f : entityDesc.fields) {
        if (f.name.equals(field.name)) {
          sb.append("    entity.set").append(capitalize(f.name))
            .append("(").append(f.name).append(");\n");
        } else {
          sb.append("    if (").append(f.name).append(".isPresent()) entity.set")
            .append(capitalize(f.name)).append("(").append(f.name).append(".get());\n");
        }
      }
      sb.append("\n");
      sb.append("    return ").append(managerVariableName).append(".")
        .append(methodName).append("(entity);\n");
      sb.append("  }\n\n");
    }

    return sb;
  }
  
  String genControllerCode(EntitySource source, EntityDesc entityDesc) {
    String controllerClassName = genControllerClassName(source.className);
    String managerClassName = genManagerClassName(source.className);

    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(source.packagePrefix).append(".api;\n\n");

    if (entityDesc.hasBigDecimalType) sb.append("import java.math.BigDecimal;\n");
    if (entityDesc.hasDateTimeType) sb.append("import java.time.*;\n");
    sb.append("import java.util.*;\n");
    sb.append("import javax.validation.Valid;\n");
    sb.append("import org.jsondoc.core.annotation.*;\n");
    sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
    sb.append("import org.springframework.validation.*;\n");
    sb.append("import org.springframework.web.bind.annotation.*;\n");
    sb.append("import org.springframework.http.*;\n");
    sb.append("import ").append(source.packagePrefix).append(".model.*;\n");
    sb.append("import ").append(source.packagePrefix).append(".entity.*;\n");
    sb.append("import ").append(source.packagePrefix).append(".manager.*;\n");
    sb.append("\n");

    sb.append("@Api(name = \"").append(source.className).append(" API\", description=\"")
      .append(source.className).append("\")\n");
    sb.append("@RestController\n");
    sb.append("@RequestMapping(\"/api\")\n");
    
    sb.append("public class ").append(controllerClassName).append(" {\n");
    sb.append("  @Autowired ").append(managerClassName).append(" ")
      .append(uncapitalize(managerClassName)).append(";\n");
    sb.append("\n");

    sb.append(genControllerSelectFun(source, entityDesc));
    sb.append("\n");
    sb.append(genControllerInsertFun(source, entityDesc));
    sb.append("\n");
    sb.append(genControllerUpdateFun(source, entityDesc));
    sb.append("}\n");
    return sb.toString();
  }

  String genManagerClassName(String className) {
    return className + "Manager";
  }

  StringBuilder genManagerSelectFun(EntitySource source, EntityDesc entityDesc) {
    String daoVariableName = uncapitalize(genDaoClassName(source.className));
    StringBuilder sb = new StringBuilder();

    for (FieldDesc field : entityDesc.uniqKeys) {
      if (field.isAutoIncrement) {
        sb.append("  public ApiResult find() {\n");
        sb.append("    return new ApiResult<List>(").append(daoVariableName).append(".find());\n");
        sb.append("  }\n\n");

        sb.append("  public ApiResult find(long pager) {\n");
        sb.append("    return new ApiResult<List>(").append(daoVariableName)
          .append(".findPage(pager));\n");
        sb.append("  }\n\n");
      }

      String methodName = "findBy" + capitalize(field.name);
      
      sb.append("  public ApiResult ").append(methodName).append("(")
        .append(field.type).append(" ").append(field.name).append(") {\n");
      sb.append("    return new ApiResult<").append(genEntityClassName(source.className))
        .append(">(").append(daoVariableName).append(".").append(methodName)
        .append("(").append(field.name).append("));\n");
      sb.append("  }\n\n");
    }
    return sb;
  }

  StringBuilder genManagerInsertFun(EntitySource source, EntityDesc entityDesc) {
    String daoVariableName = uncapitalize(genDaoClassName(source.className));
    StringBuilder sb = new StringBuilder();

    sb.append("  public ApiResult add(").append(genEntityClassName(source.className))
      .append(" entity) {\n");
    sb.append("    ").append(daoVariableName).append(".insert(entity);\n");
    if (entityDesc.isPrimaryKeyAutoIncrement) {
      sb.append("    return new ApiResult<Long>(entity.get")
        .append(capitalize(entityDesc.autoIncrementKey)).append("());\n");
    } else {
      sb.append("    return ApiResult.ok();\n");
    }
    sb.append("  }\n");
      
    return sb;
  }

  StringBuilder genManagerUpdateFun(EntitySource source, EntityDesc entityDesc) {
    String daoVariableName = uncapitalize(genDaoClassName(source.className));
    String entityClassName = genEntityClassName(source.className);
    StringBuilder sb = new StringBuilder();

    for (FieldDesc field : entityDesc.uniqKeys) {
      String method = "updateBy" + capitalize(field.name);
      sb.append("  public ApiResult ").append(method).append("(")
        .append(entityClassName).append(" entity) {\n");
      sb.append("    ").append(daoVariableName).append(".").append(method).append("(entity);\n");
      sb.append("    return ApiResult.ok();\n");
      sb.append("  }\n\n");
    }

    return sb;                                          
  }
      
  String genManagerCode(EntitySource source, EntityDesc entityDesc) {
    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(source.packagePrefix).append(".manager;\n\n");

    sb.append("import java.util.*;\n");
    sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
    sb.append("import org.springframework.stereotype.Component;\n");
    sb.append("import ").append(source.packagePrefix).append(".model.*;\n");
    sb.append("import ").append(source.packagePrefix).append(".dao.*;\n");
    sb.append("import ").append(source.packagePrefix).append(".entity.*;\n");
    sb.append("\n");

    sb.append("@Component\n");
    sb.append("public class ").append(genManagerClassName(source.className)).append(" {\n");
    sb.append("  @Autowired ").append(genDaoClassName(source.className))
      .append(" ").append(uncapitalize(genDaoClassName(source.className))).append(";\n");
    sb.append("\n");

    sb.append(genManagerSelectFun(source, entityDesc));
    sb.append("\n");
    sb.append(genManagerInsertFun(source, entityDesc));
    sb.append("\n");
    sb.append(genManagerUpdateFun(source, entityDesc));
    sb.append("}\n");
    
    return sb.toString();
  }  
  
  @ApiMethod(description = "Get Dao/Entity/Controller/Manager code")  
  @RequestMapping(value = {"/", "/{typeOpt}"}, method = RequestMethod.GET,
                  produces = "text/plain"
  )
  public String getDaoCode(
    @ApiPathParam(name = "type", description = "code type",
                  allowedvalues = {"dao", "entity", "controller", "manager", ""})
    @PathVariable Optional<String> typeOpt,
    
    @ApiQueryParam(name = "dbHostPort", description = "mysql host:port")
    @RequestParam String dbHostPort,
    
    @ApiQueryParam(name = "dbUser", description = "db username")
    @RequestParam String dbUser,
    
    @ApiQueryParam(name = "dbPassword", description = "db password")
    @RequestParam String dbPassword,
    
    @ApiQueryParam(name = "table", description = "table name")
    @RequestParam String table,
    
    @ApiQueryParam(name = "className", description = "class name tips")
    @RequestParam(required = false,  defaultValue = "") String className,

    @ApiQueryParam(name = "packagePrefix", description = "package name prefix")
    @RequestParam(required = false, defaultValue = "") String packagePrefix) {

    EntitySource source = new EntitySource();
    source.dbHostPort    = dbHostPort;
    source.dbUser        = dbUser;
    source.dbPassword    = dbPassword;
    source.table         = table;
    source.className     = className.isEmpty() ? table : className;
    source.packagePrefix = packagePrefix;

    String type = typeOpt.orElse("");
    if (type.equals("dao")) {
      return genDaoCode(source, getEntityDesc(source));
    } else if (type.equals("entity")) {
      return genEntityCode(source, getEntityDesc(source));
    } else if (type.equals("controller")) {
      return genControllerCode(source, getEntityDesc(source));
    } else if (type.equals("manager")) {
      return genManagerCode(source, getEntityDesc(source));
    } else {
      return genCode(source);
    }
  }

  String genCode(EntitySource source) {
    EntityDesc desc = getEntityDesc(source);
    
    StringBuilder sb = new StringBuilder();
    
    sb.append("== BEGIN ").append(genDaoClassName(source.className)).append(" ==\n");
    sb.append(genDaoCode(source, desc));
    sb.append("== END ").append(genDaoClassName(source.className)).append(" ==\n");

    sb.append("== BEGIN ").append(genEntityClassName(source.className)).append(" ==\n");
    sb.append(genEntityCode(source, desc));
    sb.append("== END ").append(genEntityClassName(source.className)).append(" ==\n");

    sb.append("== BEGIN ").append(genControllerClassName(source.className)).append(" ==\n");
    sb.append(genControllerCode(source, desc));
    sb.append("== END ").append(genControllerClassName(source.className)).append(" ==\n");

    sb.append("== BEGIN ").append(genManagerClassName(source.className)).append(" ==\n");
    sb.append(genManagerCode(source, desc));
    sb.append("== END ").append(genManagerClassName(source.className)).append(" ==\n");

    return sb.toString();
  }

  @ExceptionHandler(Errno.InternalErrorException.class)
  @ResponseStatus
  public String internalServerError(Exception e) {
    return e.toString();
  }
  
}
