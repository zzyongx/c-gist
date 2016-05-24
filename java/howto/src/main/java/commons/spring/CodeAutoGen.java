package commons.spring;

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
    public boolean security;
  }

  public static class FieldDesc {
    public String  name;
    public String  type;
    public boolean isPrimary;
    public boolean isAutoIncrement;
    public boolean isKey;
    public boolean timestamp;
    public boolean autoUpdate;
    public boolean isEnum;

    private FieldDesc() {
      isPrimary = isAutoIncrement = isKey = false;
      timestamp = autoUpdate = false;
      isEnum = false;
    }

    private static String detectTypeTip(String tableSql, String field) {
      String[] lines = tableSql.split("\n");
      for (String line : lines) {
        if (line.indexOf(field) != -1) {
          if (line.indexOf("COMMENT") != -1 && line.indexOf("'enum'") != -1) {
            return "enum";
          }
        }
      }
      return null;
    }

    public static FieldDesc buildFromResultSet(String tableSql, ResultSet rs) {
      FieldDesc field = new FieldDesc();
      try {
        field.name = rs.getString("Field");
        String tip = detectTypeTip(tableSql, field.name);
      
        String type = rs.getString("Type");
        String def = rs.getString("Default");

        if (tip != null) {
          if (tip.equals("enum")) {
            field.isEnum = true;
            field.type = capitalize(field.name);
          }
        } else if (type.startsWith("bigint")) {
          field.type = "long";
        } else if (type.contains("int")) {
          field.type = "int";
        } else if (type.contains("char") || type.contains("text")) {
          field.type = "String";
        } else if (type.equals("timestamp")) {
          field.type = "LocalDateTime";
          field.timestamp = true;
          field.autoUpdate = def.equals("CURRENT_TIMESTAMP");
        } else if (type.equals("datetime")) {
          field.type = "LocalDateTime";
        } else if (type.equals("date")) {
          field.type = "LocalDate";
        } else if (type.startsWith("decimal")) {
          field.type = "BigDecimal";
        }

        String key = rs.getString("Key");
        String extra = rs.getString("Extra");
        if (key.equals("PRI")) {
          field.isPrimary = field.isKey = true;
          if (extra.equals("auto_increment")) {
            field.isAutoIncrement = true;
          }
        } else if (key.equals("UNI") || key.equals("MUL")) {
          field.isKey = true;
        }
        return field;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class EntityDesc {
    public boolean hasDateTimeType;
    public boolean hasDateType;
    public boolean hasBigDecimalType;

    public boolean hasPrimaryKey;
    public boolean isPrimaryKeyAutoIncrement;
    public String  primaryKeyType;
    public String  primaryKeyName;
    
    public List<FieldDesc> fields;

    public static EntityDesc buildFromTable(EntitySource source) {
      List<String> createTableSql = execSql(
        source, "SHOW CREATE TABLE " + source.table,
        new RowMapper<String>() {
          public String mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getString("Create Table");
          }
        });

      List<FieldDesc> fields =  execSql(
        source, "DESC " + source.table,
        new RowMapper<FieldDesc>() {
          public FieldDesc mapRow(ResultSet rs, int rowNum) throws SQLException {
            return FieldDesc.buildFromResultSet(createTableSql.get(0), rs);
          }
        });

      EntityDesc entityDesc = new EntityDesc();
      entityDesc.fields = fields;
      entityDesc.prepare();
      return entityDesc;
    }

    private static <T> List<T> execSql(EntitySource source, String sql, RowMapper<T> rowMapper) {
      SingleConnectionDataSource c = new SingleConnectionDataSource(
        "jdbc:mysql://" + source.dbHostPort + "/?characterEncoding=utf-8",
        source.dbUser, source.dbPassword, false);
      JdbcTemplate jdbcTemplate = new JdbcTemplate(c);

      try {
        return jdbcTemplate.query(sql, rowMapper);
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        c.destroy();
      }
    }  
    
    private void prepare() {
      hasDateTimeType = hasDateType = false;
      hasBigDecimalType = false;
      hasPrimaryKey = isPrimaryKeyAutoIncrement = false;

      for (FieldDesc field : fields) {
        if (field.isPrimary) {
          hasPrimaryKey = true;
          primaryKeyType = field.type;
          primaryKeyName = field.name;
        }
        if (field.type.equals("LocalDateTime") || field.type.equals("LocalDate")) {
          hasDateTimeType = true;
          if (field.type.equals("LocalDate")) hasDateType = true;
        } else if (field.type.equals("BigDecimal")) {
          hasBigDecimalType = true;
        }
      }
    }

    
    
  }


  static String box(String type) {
    if (type.equals("long")) return "Long";
    else if (type.equals("int")) return "Integer";
    else return type;
  }

  String genMapperClassName(String className) {
    return className + "Mapper";
  }

  public static class CodeWriter {
    private List<String> lines = new ArrayList<>();

    public static String repeat(int n) {
      if (n == 0) return "";
      else return new String(new char[n]).replace('\0', ' ');
    }

    public CodeWriter write(String format, Object ... args) {
      return write(0, format, args);
    }

    public CodeWriter write(int padding, String format, Object ... args) {
      if (format == null) return this;
      
      format = format.replace('\'', '"');
      String space = repeat(padding);
      lines.add(space + String.format(format, args));
      return this;
    }

    public CodeWriter newLine() {
      lines.add("");
      return this;
    }

    public String toString() {
      return toString(0);
    }
    public String toString(int padding) {
      String space = repeat(padding);
      StringBuilder builder = new StringBuilder();
      for (String line : lines) {
        builder.append(space + line + "\n");
      }
      return builder.toString();
    }
  }

  String genMapperSelectSql(EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    
    CodeWriter cw = new CodeWriter();
    cw.write("static final String SELECT_BY_%s = 'SELECT * FROM ' + TABLE + ' WHERE %s = #{%s}';",
             primaryKey.toUpperCase(), primaryKey, primaryKey);

    return cw.toString(4);
  }
  
  String genMapperSelectFun(EntityDesc entityDesc, String entityClazz) {
    CodeWriter cw = new CodeWriter();
    cw.write("@SelectProvider(type = Paging.class, method = 'list')")
      .write("List<%s> find(Paging paging);", entityClazz)
      .newLine()
      .write("@SelectProvider(type = Paging.class, method = 'first')")
      .write("List<%s> first(Paging paging);", box(entityDesc.primaryKeyType))
      .newLine()
      .write("@SelectProvider(type = Paging.class, method = 'forward')")
      .write("List<%s> forward(Paging paging);", box(entityDesc.primaryKeyType))
      .newLine()
      .write("@SelectProvider(type = Paging.class, method = 'backward')")
      .write("List<%s> backward(Paging paging);", box(entityDesc.primaryKeyType))
      .newLine();

    if (entityDesc.hasPrimaryKey) {
      String primaryKey = entityDesc.primaryKeyName;
      cw.write("@Select(Sql.SELECT_BY_%s)", primaryKey.toUpperCase())
        .write("%s findBy%s(%s %s);", entityClazz, capitalize(primaryKey),
               entityDesc.primaryKeyType, primaryKey);
    }
    return cw.toString(2);
  }

  String genMapperInsertSql(EntityDesc entityDesc, String clazz) {
    String var = uncapitalize(clazz);
    CodeWriter cw = new CodeWriter();

    cw.write(0, "public static String insert(%s %s) {", clazz, var)
      .write(2, "SQL sql = new SQL().INSERT_INTO(TABLE);")
      .newLine();

    for (FieldDesc field : entityDesc.fields) {
      if (field.isAutoIncrement) continue;

      String fieldClazz = capitalize(field.name);
      String fieldVar = field.name;

      if (field.isKey) {
        cw.write(2, "sql.VALUES('%s', '#{%s}');", fieldVar, fieldVar);
      } else if (field.timestamp) {
        if (!field.autoUpdate) cw.write(2, "sql.VALUES('%s', 'NULL');", fieldVar);
      } else {
        if (field.type.equals("long")) {
          cw.write(2, "if (%s.get%s() != Long.MIN_VALUE) {", var, fieldClazz);
        } else if (field.type.equals("int")) {
          cw.write(2, "if (%s.get%s() != Integer.MIN_VALUE) {", var, fieldClazz);
        } else {
          cw.write(2, "if (%s.get%s() != null) {", var, fieldClazz);
        }
        cw.write(4, "sql.VALUES('%s', '#{%s}');", fieldVar, fieldVar)
          .write(2, "}");
      }
      
      cw.newLine();
    }
    cw.write(2, "return sql.toString();")
      .write(0, "}");
    return cw.toString(4);
  }

  String genMapperInsertFun(EntityDesc entityDesc, String clazz) {
    String var = uncapitalize(clazz);
    CodeWriter cw = new CodeWriter();

    cw.write("@InsertProvider(type = Sql.class, method = 'insert')");
    if (entityDesc.isPrimaryKeyAutoIncrement) {
      cw.write("@Options(useGeneratedKeys=true, keyProperty = '%s')", entityDesc.primaryKeyName);
    }
    cw.write("int add(%s %s);", clazz, var);
    return cw.toString(2);
  }

  String genMapperUpdateSql(EntityDesc entityDesc, String clazz) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    
    String var = uncapitalize(clazz);
    CodeWriter cw = new CodeWriter();

    cw.write(0, "public static String update(%s %s) {", clazz, var)
      .write(2, "SQL sql = new SQL().UPDATE(TABLE);")
      .newLine();
      
    for (FieldDesc f : entityDesc.fields) {
      if (f.name.equals(primaryKey) || f.timestamp) continue;

      String fieldClazz = capitalize(f.name);
      String fieldVar = f.name;
        
      if (f.type.equals("long")) {
        cw.write(2, "if (%s.get%s() != Long.MIN_VALUE) {", var, fieldClazz);
      } else if (f.type.equals("int")) {
        cw.write(2, "if (%s.get%s() != Integer.MIN_VALUE) {", var, fieldClazz);
      } else {
        cw.write(2, "if (%s.get%s() != null) {", var, fieldClazz);
      }
      cw.write(4, "sql.SET('%s = #{%s}');", var, fieldClazz)
        .write(2, "}")
        .newLine();
    }

    cw.write(2, "sql.WHERE('%s = #{%s}');", primaryKey, primaryKey)
      .write(2, "return sql.toString();")
      .write(0, "}");
    
    return cw.toString(4);
  }

  String genMapperUpdateFun(EntityDesc entityDesc, String clazz) {
    String var = uncapitalize(clazz);
    CodeWriter cw = new CodeWriter();

    cw.write("@UpdateProvider(type = Sql.class, method = 'update')")
      .write("int update(%s %s);", clazz, var);

    return cw.toString(2);
  }

  String genMapperCode(EntitySource source, EntityDesc entityDesc) {
    String mapperClazz = genMapperClassName(source.className);
    String entityClazz = genEntityClassName(source.className);

    CodeWriter cw = new CodeWriter();
    cw.write("package %s.mapper;", source.packagePrefix)
      .newLine()
      .write("import java.util.*;")
      .write("import org.apache.ibatis.annotations.*;")
      .write("import org.apache.ibatis.jdbc.SQL;");
    
    if (entityDesc.hasPrimaryKey) cw.write("import commons.mybatis.Paging;");
    
    cw.write("import %s.entity.%s;", source.packagePrefix, entityClazz)
      .newLine()
      .write("public interface %s {", mapperClazz)
      .write(2, "class Sql {")
      .write(4, "public static final String TABLE = '%s';", source.className)
      .write(0, genMapperSelectSql(entityDesc))
      .write(0, genMapperInsertSql(entityDesc, entityClazz))
      .write(0, genMapperUpdateSql(entityDesc, entityClazz))
      .write(2, "}")
      .newLine()
      .write(0, genMapperSelectFun(entityDesc, entityClazz))
      .write(0, genMapperInsertFun(entityDesc, entityClazz))
      .write(0, genMapperUpdateFun(entityDesc, entityClazz))
      .write("}");
    return cw.toString();
  }

  String genEntityClassName(String className) {
    return className;
  }
  
  String genEntityCode(EntitySource source, EntityDesc entityDesc) {
    String entityClazz = genEntityClassName(source.className);

    CodeWriter cw = new CodeWriter();
    cw.write("package %s.entity;", source.packagePrefix)
      .newLine();

    if (entityDesc.hasBigDecimalType) cw.write("import java.math.BigDecimal;");
    if (entityDesc.hasDateTimeType) cw.write("import java.time.*;");
    if (entityDesc.hasDateType) {
      cw.write("import org.springframework.format.annotation.DateTimeFormat;");
    }
    cw.write("import org.jsondoc.core.annotation.*;")
      .newLine();

    cw.write("@ApiObject(name = '%s', description = '%s')", entityClazz, entityClazz);
    cw.write("public class %s {", entityClazz);

    for (FieldDesc field : entityDesc.fields) {
      if (!field.isEnum) continue;
      cw.write(2, "public static enum %s {", field.type)
        .write(4, "PH(1);")
        .newLine()
        .write(4, "private int value;")
        .write(4, "%s(int value) {this.value = value;}", field.type)
        .write(4, "public int getValue() {return this.value;}")
        .write(2, "}")
        .newLine();
    }
      
    for (FieldDesc field : entityDesc.fields) {
      cw.write(2, "@ApiObjectField(description = '%s')", field.name);
      if (field.type.equals("long")) {
        cw.write(2, "%s %s = Long.MIN_VALUE;", field.type, field.name);
      } else if (field.type.equals("int")) {
        cw.write(2, "%s %s = Integer.MIN_VALUE;", field.type, field.name);
      } else {
        if (field.type.equals("LocalDate")) {
          cw.write(2, "@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)");
        }
        cw.write(2, "%s %s;", field.type, field.name);
      }
      cw.newLine();
    }
    
    for (FieldDesc field : entityDesc.fields) {
      cw.write(2, "public void set%s(%s %s) {", capitalize(field.name), field.type, field.name)
        .write(4, "this.%s = %s;", field.name, field.name)
        .write(2, "}")
        .write(2, "public %s get%s() {", field.type, capitalize(field.name))
        .write(4, "return this.%s;", field.name)
        .write(2, "}")
        .newLine();
    }

    cw.write("}");
    return cw.toString();
  }

  String genControllerClassName(String className) {
    return className + "Controller";
  }

  String genControllerSelectFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    
    String managerVar = uncapitalize(genManagerClassName(source.className));
    String entityClazz = source.className;
    String entityVar = uncapitalize(source.className);
    CodeWriter cw = new CodeWriter();

    cw.write("@ApiMethod(description = 'get first %s pages')", entityClazz)
      .write("@RequestMapping(value = '/%s/pages', method = RequestMethod.GET)", entityVar)
      .write("public ApiResult pages() {")
      .write(2, "return %s.pages(-1, true, pages, count);", managerVar)
      .write("}")
      .newLine();

    cw.write("@ApiMethod(description = 'get %s pages backward')", entityClazz)
      .write("@RequestMapping(value = '/%s/pages/backward', method = RequestMethod.GET)", entityVar)
      .write("public ApiResult backwardPages(")
      .write(2, "@ApiQueryParam(name = '%s', description = 'the current smallest %s')",
             primaryKey, primaryKey)
      .write(2, "@RequestParam %s %s) {", entityDesc.primaryKeyType, primaryKey)
      .write(2, "return %s.pages(%s, true, pages, count);", managerVar, primaryKey)
      .write("}")
      .newLine();
   
    cw.write("@ApiMethod(description = 'get %s pages forward')", entityClazz)
      .write("@RequestMapping(value = '/%s/pages/forward', method = RequestMethod.GET)", entityVar)
      .write("public ApiResult forwardPages(")
      .write(2, "@ApiQueryParam(name = '%s', description = 'the current bigest %s')",
             primaryKey, primaryKey)
      .write(2, "@RequestParam %s %s) {", entityDesc.primaryKeyType, primaryKey)
      .write(2, "return %s.pages(%s, true, pages, count);", managerVar, primaryKey)
      .write("}")
      .newLine();

    cw.write("@ApiMethod(description = 'browse %ss')", entityClazz)
      .write("@RequestMapping(value = '/%s/list', method = RequestMethod.GET)", entityVar)
      .write("public ApiResult list%s(", entityClazz)
      .write(2, "@ApiQueryParam(name = '%s', description = 'if first screen, ignore')", primaryKey)
      .write(2, "@RequestParam Optional<%s> %s) {", box(entityDesc.primaryKeyType), primaryKey)
      .write(2, "return %s.list(%s, count);", managerVar, primaryKey)
      .write("}")
      .newLine();
    
    cw.write("@ApiMethod(description = 'FunName: find %s by %s')", entityClazz, primaryKey)
      .write("@RequestMapping(value = '/%s/{%s}', method = RequestMethod.GET)",
             entityVar, primaryKey)
      .write("public ApiResult findBy%s(", capitalize(primaryKey))
      .write(2, "@ApiPathParam(name = '%s', description = '%s')", primaryKey, primaryKey)
      .write(2, "@PathVariable %s %s) {", entityDesc.primaryKeyType, primaryKey)
      .write(2, "return %s.findBy%s(%s);", managerVar, capitalize(primaryKey), primaryKey)
      .write("}");
    
    return cw.toString(2);
  }

  String genControllerInsertFun(EntitySource source, EntityDesc entityDesc) {
    String entityClazz = genEntityClassName(source.className);
    String entityVar = uncapitalize(entityClazz);
    String managerVar = uncapitalize(genManagerClassName(source.className));
    CodeWriter cw = new CodeWriter();

    cw.write("@ApiMethod(description = 'FunName: add %s')", source.className)
      .write("@RequestMapping(value = '/%s', method = RequestMethod.POST)",
             uncapitalize(source.className))
      .write("public ApiResult add(");
    if (source.security) {
      cw.write(2, "@AuthenticationPrincipal RedisRememberMeService.User user,");
    }
    cw.write(2, "@ApiBodyObject @Valid %s %s,", entityClazz, entityVar)
      .write(2, "BindingResult bindingResult) {")
      .write(2, "if (bindingResult.hasErrors()) {")
      .write(4, "return ApiResult.bindingResult(bindingResult);")
      .write(2, "}")
      .newLine()
      .write(2, "return %s.add(%s);", managerVar, entityVar)
      .write("}");

    return cw.toString(2);
  }

  String genControllerUpdateFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return "";
    String primaryKey = entityDesc.primaryKeyName;
    
    String entityClazz = genEntityClassName(source.className);
    String entityVar = uncapitalize(entityClazz);
    String managerVar = uncapitalize(genManagerClassName(source.className));
    CodeWriter cw = new CodeWriter();

    cw.write("@ApiMethod(description = 'FunName: update')");
    cw.write("@RequestMapping(value = '/%s/{%s}', method = RequestMethod.PUT)",
             uncapitalize(source.className), primaryKey);
    cw.write("public ApiResult update(");
    if (source.security) {
      cw.write(2, "@AuthenticationPrincipal RedisRememberMeService.User user,");
    }

    for (FieldDesc f : entityDesc.fields) {
      if (f.name.equals(primaryKey) || (f.timestamp && f.autoUpdate)) continue;

      cw.write(2, "@ApiQueryParam(name = '%s', description = '%s', required = false)",
               f.name, f.name);

      if (f.type.equals("LocalDate")) {
        cw.write(2, "@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)")
          .write(2, "@RequestParam Optional<LocalDate> %s,", f.name);
      } else if (f.isEnum) {
        cw.write(2, "@RequestParam Optional<%s.%s> %s,", entityClazz, f.type, f.name);
      } else {
        cw.write(2, "@RequestParam Optional<%s> %s,", box(f.type), f.name);
      }
      cw.newLine();
    }

    cw.write(2, "@ApiPathParam(name = '%s', description = '%s')", primaryKey, primaryKey)
      .write(2, "@PathVariable %s %s) {", entityDesc.primaryKeyType, primaryKey)
      .newLine()
      .write(2, "%s %s = new %s();", entityClazz, entityVar, entityClazz);
               
    for (FieldDesc f : entityDesc.fields) {
      if (f.timestamp && f.autoUpdate) continue;
      
      if (f.name.equals(primaryKey)) {
        cw.write(2, "%s.set%s(%s);", entityVar, capitalize(f.name), f.name);
      } else if (f.type.equals("long") || f.type.equals("int")) {
        cw.write(2, "if (%s.isPresent()) %s.set%s(%s.get());",
                 f.name, entityVar, capitalize(f.name), f.name);
      } else {
        cw.write(2, "%s.set%s(%s.orElse(null));", entityVar, capitalize(f.name), f.name);
      }
    }
    cw.newLine()
      .write(2, "return %s.update(%s);", managerVar, entityVar)
      .write("}");

    return cw.toString(2);
  }
  
  String genControllerCode(EntitySource source, EntityDesc entityDesc) {
    String controllerClazz = genControllerClassName(source.className);
    String managerClazz = genManagerClassName(source.className);
    CodeWriter cw = new CodeWriter();

    cw.write("package %s.api;", source.packagePrefix)
      .newLine();

    if (entityDesc.hasBigDecimalType) cw.write("import java.math.BigDecimal;");
    if (entityDesc.hasDateTimeType) cw.write("import java.time.*;");
    cw.write("import java.util.*;");
    cw.write("import javax.validation.Valid;");
    cw.write("import org.jsondoc.core.annotation.*;");
    cw.write("import org.springframework.beans.factory.annotation.Autowired;");
    cw.write("import org.springframework.validation.*;");
    cw.write("import org.springframework.web.bind.annotation.*;");
    if (entityDesc.hasDateType) {
      cw.write("import org.springframework.format.annotation.DateTimeFormat;");
    }
    if (entityDesc.hasPrimaryKey) {
      cw.write("import org.springframework.core.env.Environment;");
    }
    cw.write("import org.springframework.http.*;");
    if (source.security) {
      cw.write("import org.springframework.security.core.annotation.AuthenticationPrincipal;");
      cw.write("import commons.spring.RedisRememberMeService;");
    }
    cw.write("import %s.model.*;", source.packagePrefix);
    cw.write("import %s.entity.*;", source.packagePrefix);
    cw.write("import %s.manager.*;", source.packagePrefix);
    cw.newLine();

    cw.write("@Api(name = '%s API', description = '%s')", source.className, source.className)
      .write("@RestController")
      .write("@RequestMapping('/api')")
      .write("public class %s {", controllerClazz)
      .write(2, "@Autowired %s %s;", managerClazz, uncapitalize(managerClazz))
      .newLine();
    
    if (entityDesc.hasPrimaryKey) {
      cw.write(2, "private int pages;")
        .write(2, "private int count;")
        .newLine()
        .write(2, "@Autowired")
        .write(2, "public %s(Environment env) {", controllerClazz)
        .write(4, "this.pages = Integer.parseInt(env.getProperty('list.pages', '10'));")
        .write(4, "this.count = Integer.parseInt(env.getProperty('list.count', '20'));")
        .write(2, "}")
        .newLine();
    }

    cw.write(genControllerSelectFun(source, entityDesc))
      .write(genControllerInsertFun(source, entityDesc))
      .write(genControllerUpdateFun(source, entityDesc))
      .write("}");
    
    return cw.toString();
  }

  String genManagerClassName(String className) {
    return className + "Manager";
  }

  String genManagerSelectFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;

    String entityClazz = genEntityClassName(source.className);
    String entityVar = uncapitalize(entityClazz);
    String mapperClazz = genMapperClassName(source.className);
    String mapperVar = uncapitalize(mapperClazz);
    CodeWriter cw = new CodeWriter();

    cw.write("public ApiResult pages(%s %s, boolean backward, int pages, int count) {",
             entityDesc.primaryKeyType, primaryKey)
      .write(2, "Paging paging = new Paging().setTable(%s.Sql.TABLE);", mapperClazz)
      .write(2, "paging.setRowId('%s').setCount(pages, count);", primaryKey)
      .newLine()
      .write(2, "List<%s> %ss;", box(entityDesc.primaryKeyType), entityVar)
      .write(2, "if (%s == -1) {", primaryKey)
      .write(4, "%ss = %s.first(paging);", entityVar, mapperVar)
      .write(2, "} else {")
      .write(4, "paging.setParams('%s', %s);", primaryKey, primaryKey)
      .write(4, "%ss = backward ? %s.backward(paging) : %s.forward(paging);",
             entityVar, mapperVar, mapperVar)
      .write(2, "}")
      .write(2, "return new ApiResult<List>(paging.pages(%ss, count));", entityVar)
      .write("}")
      .newLine();
    
    cw.write("public ApiResult list(Optional<%s> %s, int count) {",
             box(entityDesc.primaryKeyType), primaryKey)
      .write(2, "Paging paging = new Paging().setTable(%s.Sql.TABLE);", mapperClazz)
      .write(2, "paging.setRowId('%s').setCount(1, count);", primaryKey)
      .newLine()
      .write(2, "List<%s> %ss;", entityClazz, entityVar)
      .write(2, "if (%s.isPresent()) paging.setParams('%s', %s.get());",
             primaryKey, primaryKey, primaryKey)
      .write(2, "%ss = %s.find(paging);", entityVar, mapperVar)
      .write(2, "return new ApiResult<List>(%ss);", entityVar)
      .write("}")
      .newLine();

    cw.write("public ApiResult findBy%s(%s %s) {",
             capitalize(primaryKey), entityDesc.primaryKeyType, primaryKey)
      .write(2, "%s %s = %s.findBy%s(%s);", entityClazz, entityVar, mapperVar,
             capitalize(primaryKey), primaryKey)
      .write(2, "return new ApiResult<%s>(%s);", entityClazz, entityVar)
      .write("}");

    return cw.toString(2);
  }

  String genManagerInsertFun(EntitySource source, EntityDesc entityDesc) {
    String entityClazz = genEntityClassName(source.className);
    String entityVar = uncapitalize(entityClazz);
    String mapperVar = uncapitalize(genMapperClassName(source.className));
    CodeWriter cw = new CodeWriter();

    cw.write("public ApiResult add(%s %s) {", entityClazz, entityVar)
      .write(2, "%s.add(%s);", mapperVar, entityVar)
      .write(2, "return new ApiResult<%s>(%s);", entityClazz, entityVar)
      .write("}");
      
    return cw.toString(2);
  }

  String genManagerUpdateFun(EntitySource source, EntityDesc entityDesc) {
    String entityClazz = genEntityClassName(source.className);
    String entityVar = uncapitalize(entityClazz);
    String mapperVar = uncapitalize(genMapperClassName(source.className));
    CodeWriter cw = new CodeWriter();

    cw.write("public ApiResult update(%s %s) {", entityClazz, entityVar)
      .write(2, "%s.update(%s);", mapperVar, entityVar)
      .write(2, "return ApiResult.ok();")
      .write("}");

    return cw.toString(2); 
  }
      
  String genManagerCode(EntitySource source, EntityDesc entityDesc) {
    String mapperClazz = genMapperClassName(source.className);
    String mapperVar = uncapitalize(mapperClazz);
    CodeWriter cw = new CodeWriter();

    cw.write("package %s.manager;", source.packagePrefix)
      .newLine()
      .write("import java.util.*;")
      .write("import org.springframework.beans.factory.annotation.Autowired;")
      .write("import org.springframework.stereotype.Component;");
    
    if (entityDesc.hasPrimaryKey) cw.write("import commons.mybatis.Paging;");
    
    cw.write("import %s.model.*;", source.packagePrefix)
      .write("import %s.mapper.*;", source.packagePrefix)
      .write("import %s.entity.*;", source.packagePrefix)
      .newLine();

    cw.write("@Component");
    cw.write("public class %s {", genManagerClassName(source.className))
      .write(2, "@Autowired %s %s;", mapperClazz, mapperVar)
      .newLine()
      .write(genManagerSelectFun(source, entityDesc))
      .write(genManagerInsertFun(source, entityDesc))
      .write(genManagerUpdateFun(source, entityDesc))
      .write("}");
    
    return cw.toString();
  }  
  
  @ApiMethod(description = "Get Mapper/Entity/Controller/Manager code")  
  @RequestMapping(value = {"/", "/{typeOpt}"}, method = RequestMethod.GET,
                  produces = "text/plain"
  )
  public String getMapperCode(
    @ApiPathParam(name = "type", description = "code type",
                  allowedvalues = {"mapper", "entity", "api", "manager", ""})
    @PathVariable Optional<String> typeOpt,
    
    @ApiQueryParam(name = "dbHostPort", description = "mysql host:port")
    @RequestParam String dbHostPort,
    
    @ApiQueryParam(name = "dbUser", description = "db username")
    @RequestParam String dbUser,
    
    @ApiQueryParam(name = "dbPassword", description = "db password")
    @RequestParam String dbPassword,
    
    @ApiQueryParam(name = "table", description = "table name")
    @RequestParam String table,
    
    @ApiQueryParam(name = "className", description = "class name tips", required = false)
    @RequestParam Optional<String> className,

    @ApiQueryParam(name = "packagePrefix", description = "package name prefix", required = false)
    @RequestParam Optional<String> packagePrefix,

    @ApiQueryParam(name = "security", description = "security flag, default yes", required = false)
    @RequestParam Optional<Boolean> security) {

    EntitySource source = new EntitySource();
    source.dbHostPort    = dbHostPort;
    source.dbUser        = dbUser;
    source.dbPassword    = dbPassword;
    source.table         = table;
    source.className     = className.orElse(table);
    source.packagePrefix = packagePrefix.orElse("example");
    source.security      = security.orElse(true);

    String type = typeOpt.orElse("");
    if (type.equals("mapper")) {
      return genMapperCode(source, EntityDesc.buildFromTable(source));
    } else if (type.equals("entity")) {
      return genEntityCode(source, EntityDesc.buildFromTable(source));
    } else if (type.equals("api")) {
      return genControllerCode(source, EntityDesc.buildFromTable(source));
    } else if (type.equals("manager")) {
      return genManagerCode(source, EntityDesc.buildFromTable(source));
    } else {
      return genCode(source);
    }
  }

  String genCode(EntitySource source) {
    EntityDesc desc = EntityDesc.buildFromTable(source);
    
    StringBuilder builder = new StringBuilder();
    
    builder.append("== BEGIN ").append(genMapperClassName(source.className)).append(" ==\n");
    builder.append(genMapperCode(source, desc));
    builder.append("== END ").append(genMapperClassName(source.className)).append(" ==\n");

    builder.append("== BEGIN ").append(genEntityClassName(source.className)).append(" ==\n");
    builder.append(genEntityCode(source, desc));
    builder.append("== END ").append(genEntityClassName(source.className)).append(" ==\n");

    builder.append("== BEGIN ").append(genControllerClassName(source.className)).append(" ==\n");
    builder.append(genControllerCode(source, desc));
    builder.append("== END ").append(genControllerClassName(source.className)).append(" ==\n");

    builder.append("== BEGIN ").append(genManagerClassName(source.className)).append(" ==\n");
    builder.append(genManagerCode(source, desc));
    builder.append("== END ").append(genManagerClassName(source.className)).append(" ==\n");

    return builder.toString();
  }

  @ApiMethod(description = "AutoCode Help")  
  @RequestMapping(value = "/help", method = RequestMethod.GET, produces = "text/plain")
  public String help() {
    StringBuilder builder = new StringBuilder();
    builder.append("Example Table: autoCode\n");
    builder.append("  CREATE TABLE autoCode (\n");
    builder.append("    codeId   INT AUTO_INCREMENT PRIMARY KEY,\n");
    builder.append("    language TINYINT COMMENT 'enum',\n");
    builder.append("    author   VARCHAR(32) NOT NULL,\n");
    builder.append("    linage   INT,\n");
    builder.append("    content  TEXT,\n");
    builder.append("    createAt TIMESTAMP NOT NULL DEFAULT 0,\n");
    builder.append("    updateAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n");
    builder.append("    expireAt DATE,\n");
    builder.append("    INDEX(author)\n");
    builder.append("  );\n\n");

    builder.append("If TINYINT with comment 'enum', it will be treat as enum type\n");
    builder.append("\n");

    builder.append("API options\n");
    builder.append("  dbHostPort    required\n");
    builder.append("  dbUser        required\n");
    builder.append("  dbPassword    required\n");
    builder.append("  table         optional format(db.table)\n");
    builder.append("  className     optional default use table's name\n");
    builder.append("  packagePrefix optional format(com.example) default example\n");
    builder.append("  security      optional format(yes|no) default yes\n");

    return builder.toString();
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus
  public String internalServerError(Exception e) {
    return e.toString();
  }
}
