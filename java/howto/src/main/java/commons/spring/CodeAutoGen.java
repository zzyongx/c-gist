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
    public String shortTable;
    public String className;
    public String packagePrefix;
    public boolean security;
    public boolean nontrans;
    public boolean nopaging;
    public boolean nopublic;

    public String entityClazz;
    public String entityVar;
    public String mapperClazz;
    public String mapperVar;
    public String managerClazz;
    public String managerVar;
    public String controllerClazz;
  }

  public static class FieldDesc {
    public String  name;
    public String  type;
    public String  size;
    public boolean isPrimary;
    public boolean isAutoIncrement;
    public boolean isKey;
    public boolean timestamp;
    public boolean autoUpdate;
    
    public boolean isEnum;
    public boolean isImmut;
    public boolean isUid;
    public boolean isDelay;
    public boolean isInternal;

    private FieldDesc() {
      isPrimary = isAutoIncrement = isKey = false;
      timestamp = autoUpdate = false;
      isEnum = isImmut = isUid = isDelay = isInternal = false;
    }

    private void fixWithTips(String tableSql, String field) {
      String[] lines = tableSql.split("\n");
      for (String line : lines) {
        int tipStart;
        if (line.indexOf('`' + field + '`') != -1 && (tipStart = line.indexOf("COMMENT")) != -1) {
          int start = line.indexOf('\'', tipStart);
          int end   = start == -1 ? -1 : line.indexOf('\'', start+1);
          if (start == -1 || end == -1) return;

          String tips[] = line.substring(start+1, end).split(",");
          for (String tip : tips) {
            if (tip.equals("enum")) {
              this.isEnum = true;
              this.type = capitalize(this.name);
            } else if (tip.indexOf("class#") != -1) {
              int typeStart = tip.indexOf('#');
              this.type = tip.substring(typeStart+1);
            } else if (tip.equals("immut")) {
              this.isImmut = true;
            } else if (tip.equals("EN")) {
              this.size = String.valueOf(Integer.parseInt(this.size) * 2);
            } else if (tip.equals("delay")) {
              this.isDelay = true;
            } else if (tip.equals("uid")) {
              this.isUid = true;
            } else if (tip.equals("internal")) {
              this.isInternal = true;
            }
          }
        }
      }
    }

    public static FieldDesc buildFromResultSet(String tableSql, ResultSet rs) {
      FieldDesc field = new FieldDesc();
      try {
        field.name = rs.getString("Field");
      
        String type = rs.getString("Type");
        String def = rs.getString("Default");

        if (type.startsWith("bigint")) {
          field.type = "long";
        } else if (type.contains("int")) {
          field.type = "int";
        } else if (type.contains("char")) {
          int bracketStart = type.indexOf('(');
          int bracketEnd   = type.indexOf(')');
          if (bracketStart != -1 && bracketEnd != -1) {
            int size = Integer.parseInt(type.substring(bracketStart+1, bracketEnd));
            field.size = String.valueOf(size/2);
          }
          field.type = "String";
        } else if (type.contains("text")) {
          field.type = "String";
        } else if (type.contains("blob")) {
          field.type = "byte[]";
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
        } else {
          throw new RuntimeException("unknow filed type: " + type);
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

        field.fixWithTips(tableSql, field.name);
        return field;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class EntityDesc {
    public boolean hasStringType;
    public boolean hasDateTimeType;
    public boolean hasDateType;
    public boolean hasBigDecimalType;
    public boolean hasBinary;

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
      hasStringType = false;
      hasDateTimeType = hasDateType = false;
      hasBigDecimalType = false;
      hasBinary = false;
      hasPrimaryKey = isPrimaryKeyAutoIncrement = false;

      for (FieldDesc field : fields) {
        if (field.isPrimary) {
          hasPrimaryKey = true;
          primaryKeyType = field.type;
          primaryKeyName = field.name;
          isPrimaryKeyAutoIncrement = field.isAutoIncrement;
        }
        if (field.type.equals("LocalDateTime") || field.type.equals("LocalDate")) {
          hasDateTimeType = true;
          if (field.type.equals("LocalDate")) hasDateType = true;
        } else if (field.type.equals("BigDecimal")) {
          hasBigDecimalType = true;
        } else if (field.type.equals("byte[]")) {
          hasBinary = true;
        } else if (field.type.equals("String")) {
          hasStringType = true;
        }
      }
    }
  }

  static String box(String type) {
    if (type.equals("long")) return "Long";
    else if (type.equals("int")) return "Integer";
    else return type;
  }

  static String box(String type, String value) {
    if (type.equals("long")) return value + "L";
    else return value;
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
    cw.write("static final String SELECT_BY_%s = 'SELECT * FROM ' + TABLE +", primaryKey.toUpperCase())
      .write(2, "' WHERE %s = #{%s}';", primaryKey, primaryKey);

    return cw.toString(4);
  }

  String genMapperSelectFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
      
    CodeWriter cw = new CodeWriter();
    
    if (!source.nopaging) {
      cw.write("@SelectProvider(type = Paging.class, method = 'list')")
        .write("List<%s> find(Paging paging);", source.entityClazz)
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
    }

    cw.write("@Select(Sql.SELECT_BY_%s)", primaryKey.toUpperCase())
      .write("%s findBy%s(%s %s);", source.entityClazz, capitalize(primaryKey),
             entityDesc.primaryKeyType, primaryKey);

    return cw.toString(2);
  }

  String genMapperInsertSql(EntitySource source, EntityDesc entityDesc) {
    CodeWriter cw = new CodeWriter();

    cw.write(0, "public static String insert(%s %s) {", source.entityClazz, source.entityVar)
      .write(2, "SQL sql = new SQL().INSERT_INTO(TABLE);")
      .newLine();

    for (FieldDesc field : entityDesc.fields) {
      if (field.isAutoIncrement || field.isDelay) continue;
      String fieldVar = field.name;

      if (field.isKey || field.isUid || field.isImmut) {
        cw.write(2, "sql.VALUES('%s', '#{%s}');", fieldVar, fieldVar);
      } else if (field.timestamp) {
        if (!field.autoUpdate) cw.write(2, "sql.VALUES('%s', 'NULL');", fieldVar);
      } 
    }
    cw.newLine();

    for (FieldDesc field : entityDesc.fields) {
      if (field.isAutoIncrement || field.isDelay) continue;
      String fieldClazz = capitalize(field.name);
      String fieldVar = field.name;

      if (!field.isKey && !field.isUid && !field.isImmut && !field.timestamp) {
        if (field.type.equals("long")) {
          cw.write(2, "if (%s.get%s() != Long.MIN_VALUE) {", source.entityVar, fieldClazz);
        } else if (field.type.equals("int")) {
          cw.write(2, "if (%s.get%s() != Integer.MIN_VALUE) {", source.entityVar, fieldClazz);
        } else {
          cw.write(2, "if (%s.get%s() != null) {", source.entityVar, fieldClazz);
        }
        cw.write(4, "sql.VALUES('%s', '#{%s}');", fieldVar, fieldVar)
          .write(2, "}")
          .newLine();
      }
    }

    cw.write(2, "return sql.toString();")
      .write(0, "}");
    return cw.toString(4);
  }

  String genMapperInsertFun(EntitySource source, EntityDesc entityDesc) {
    CodeWriter cw = new CodeWriter();

    cw.write("@InsertProvider(type = Sql.class, method = 'insert')");
    if (entityDesc.isPrimaryKeyAutoIncrement) {
      cw.write("@Options(useGeneratedKeys=true, keyProperty = '%s')", entityDesc.primaryKeyName);
    }
    cw.write("int add(%s %s);", source.entityClazz, source.entityVar);
    return cw.toString(2);
  }

  String genMapperUpdateSql(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    CodeWriter cw = new CodeWriter();

    cw.write(0, "public static String update(%s %s) {", source.entityClazz, source.entityVar)
      .write(2, "SQL sql = new SQL().UPDATE(TABLE);")
      .newLine();
      
    for (FieldDesc f : entityDesc.fields) {
      if (f.name.equals(primaryKey) || f.timestamp ||
          f.isImmut || f.isUid) continue;

      String fieldClazz = capitalize(f.name);
      String fieldVar = f.name;
        
      if (f.type.equals("long")) {
        cw.write(2, "if (%s.get%s() != Long.MIN_VALUE) {", source.entityVar, fieldClazz);
      } else if (f.type.equals("int")) {
        cw.write(2, "if (%s.get%s() != Integer.MIN_VALUE) {", source.entityVar, fieldClazz);
      } else {
        cw.write(2, "if (%s.get%s() != null) {", source.entityVar, fieldClazz);
      }
      cw.write(4, "sql.SET('%s = #{%s}');", fieldVar, fieldVar)
        .write(2, "}")
        .newLine();
    }

    cw.write(2, "sql.WHERE('%s = #{%s}');", primaryKey, primaryKey)
      .write(2, "return sql.toString();")
      .write(0, "}");
    
    return cw.toString(4);
  }

  String genMapperUpdateFun(EntitySource source) {
    CodeWriter cw = new CodeWriter();

    cw.write("@UpdateProvider(type = Sql.class, method = 'update')")
      .write("int update(%s %s);", source.entityClazz, source.entityVar);

    return cw.toString(2);
  }

  String genMapperDeleteSql(EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    
    CodeWriter cw = new CodeWriter();
    cw.write("static final String DELETE_BY_%s = 'DELETE FROM ' + TABLE +", primaryKey.toUpperCase())
      .write(2, "' WHERE %s = #{%s}';", primaryKey, primaryKey);

    return cw.toString(4);
  }

  String genMapperDeleteFun(EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;

    CodeWriter cw = new CodeWriter();
    cw.write("@Delete(Sql.DELETE_BY_%s)", primaryKey.toUpperCase())
      .write("int delete(%s %s);", entityDesc.primaryKeyType, primaryKey);

    return cw.toString(2);
  }

  String genMapperCode(EntitySource source, EntityDesc entityDesc) {
    CodeWriter cw = new CodeWriter();
    cw.write("package %s.mapper;", source.packagePrefix)
      .newLine()
      .write("import java.util.*;")
      .write("import org.apache.ibatis.annotations.*;")
      .write("import org.apache.ibatis.jdbc.SQL;");
    
    if (entityDesc.hasPrimaryKey && !source.nopaging) {
      cw.write("import commons.mybatis.Paging;");
    }
    
    cw.write("import %s.entity.%s;", source.packagePrefix, source.entityClazz)
      .newLine()
      .write("public interface %s {", source.mapperClazz)
      .write(2, "class Sql {")
      .write(4, "public static final String TABLE = '%s';", source.shortTable)
      .write(0, genMapperSelectSql(entityDesc))
      .write(0, genMapperInsertSql(source, entityDesc))
      .write(0, genMapperUpdateSql(source, entityDesc))
      .write(0, genMapperDeleteSql(entityDesc))
      .write(2, "}")
      .newLine()
      .write(0, genMapperSelectFun(source, entityDesc))
      .write(0, genMapperInsertFun(source, entityDesc))
      .write(0, genMapperUpdateFun(source))
      .write(0, genMapperDeleteFun(entityDesc))
      .write("}");
    return cw.toString();
  }

  String genEntityClassName(String className) {
    return className;
  }
  
  String genEntityCode(EntitySource source, EntityDesc entityDesc) {
    CodeWriter cw = new CodeWriter();
    cw.write("package %s.entity;", source.packagePrefix)
      .newLine();

    if (entityDesc.hasBigDecimalType) cw.write("import java.math.BigDecimal;");
    if (entityDesc.hasDateTimeType) cw.write("import java.time.*;");
    cw.write("import javax.validation.constraints.*;");
    if (entityDesc.hasDateType) {
      cw.write("import org.springframework.format.annotation.DateTimeFormat;");
    }
    cw.write("import org.jsondoc.core.annotation.*;");
    if (entityDesc.hasStringType) {
      cw.write("import commons.utils.XssHelper;");
    }
    cw.newLine();

    cw.write("@ApiObject(name = '%s', description = '%s')", source.entityClazz, source.entityClazz);
    cw.write("public class %s {", source.entityClazz);

    for (FieldDesc field : entityDesc.fields) {
      if (!field.isEnum) continue;
      cw.write(2, "@ApiObject(name = '%s.%s', description = '%s %s')",
               source.entityClazz, field.type, source.entityClazz, field.type)
        .write(2, "public static enum %s {", field.type)
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
        } else if (field.type.equals("String") && field.size != null) {
          cw.write(2, "@Size(max = %s)", field.size);
        }
        cw.write(2, "%s %s;", field.type, field.name);
      }
      cw.newLine();
    }

    if (entityDesc.hasStringType) {
      cw.write(2, "public void makeXssSafe() {");
      for (FieldDesc field : entityDesc.fields) {
        if (field.type.equals("String")) {
          cw.write(4, "%s = XssHelper.escape(%s);", field.name, field.name);
        }
      }
      cw.write(2, "}");
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

    cw.write(2, "public String toString() {");
    cw.write(4, "StringBuilder builder = new StringBuilder();")
      .write(4, "builder.append('{');")
      .newLine();
    
    boolean first = true;
    for (FieldDesc field : entityDesc.fields) {
      if (!first) cw.write(4, "builder.append('; ');").newLine();
      first = false;
      
      if (field.type.equals("long") || field.type.equals("int") || field.type.equals("String")) {
        cw.write(4, "builder.append('%s = ').append(%s);", field.name, field.name);
      } else {
        cw.write(4, "builder.append('%s = ').append(%s.toString());", field.name, field.name);
      }
    }
    cw.write(4, "builder.append('}');")
      .newLine()
      .write(4, "return builder.toString();")
      .write(2, "}");

    cw.write("}");
    return cw.toString();
  }

  String genControllerClassName(String className) {
    return className + "Controller";
  }

  void genControllerPaging(CodeWriter cw, EntitySource source, EntityDesc entityDesc) {
    if (source.nopaging) return;

    String primaryKey = entityDesc.primaryKeyName;
    
    cw.write("@ApiMethod(description = 'FunName: get %s pages')", source.entityClazz)
      .write("@RequestMapping(value = '/%s/pages', method = RequestMethod.GET)", source.entityVar)
      .write("public ApiResult %sPages(", source.entityVar)
      .write(2, "@ApiQueryParam(name = '%s', description = 'if backward, the current smallest %s orElse the bigest, default -1, the first pages')",
             primaryKey, primaryKey)
      .write(2, "@RequestParam Optional<%s> %s,", box(entityDesc.primaryKeyType), primaryKey)
      .write(2, "@ApiQueryParam(name = 'backward', description = 'default yes')")
      .write(2, "@RequestParam Optional<Boolean> backward) {")
      .write(2, "return %s.pages(%s.orElse(%s), backward.orElse(true), pages, count);",
             source.managerVar, primaryKey, box(entityDesc.primaryKeyType, "-1"))
      .write("}")
      .newLine();

    cw.write("@ApiMethod(description = 'FunName: browse %ss')", source.entityClazz)
      .write("@RequestMapping(value = '/%s/list', method = RequestMethod.GET)", source.entityVar)
      .write("public ApiResult list%s(", source.entityClazz)
      .write(2, "@ApiQueryParam(name = '%s', description = 'if first screen, ignore')", primaryKey)
      .write(2, "@RequestParam Optional<%s> %s) {", box(entityDesc.primaryKeyType), primaryKey)
      .write(2, "return %s.list(%s, count);", source.managerVar, primaryKey)
      .write("}")
      .newLine();
  }

  String genControllerSelectFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    CodeWriter cw = new CodeWriter();
    
    genControllerPaging(cw, source, entityDesc);
    
    cw.write("@ApiMethod(description = 'FunName: find %s by %s')", source.entityClazz, primaryKey)
      .write("@RequestMapping(value = '/%s/{%s}', method = RequestMethod.GET)",
             source.entityVar, primaryKey)
      .write("public ApiResult findBy%s(", capitalize(primaryKey))
      .write(2, "@ApiPathParam(name = '%s', description = '%s')", primaryKey, primaryKey)
      .write(2, "@PathVariable %s %s) {", entityDesc.primaryKeyType, primaryKey)
      .write(2, "return %s.findBy%s(%s);", source.managerVar, capitalize(primaryKey), primaryKey)
      .write("}");
    
    return cw.toString(2);
  }


  String genControllerInsertFun(EntitySource source) {
    CodeWriter cw = new CodeWriter();

    cw.write("@ApiMethod(description = 'FunName: add %s')", source.className)
      .write("@RequestMapping(value = '/%s', method = RequestMethod.POST)", source.entityVar)
      .write("public ApiResult add(");
    if (source.security) {
      cw.write(2, "@AuthenticationPrincipal RedisRememberMeService.User user,");
    }
    cw.write(2, "@ApiBodyObject @Valid %s %s,", source.entityClazz, source.entityVar)
      .write(2, "BindingResult bindingResult) {")
      .write(2, "if (bindingResult.hasErrors()) {")
      .write(4, "return ApiResult.bindingResult(bindingResult);")
      .write(2, "}")
      .newLine()
      .write(2, "// TODO: setUid()")
      .write(2, "return %s.add(%s);", source.managerVar, source.entityVar)
      .write("}");

    return cw.toString(2);
  }

  String genControllerUpdateFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    CodeWriter cw = new CodeWriter();

    cw.write("@ApiMethod(description = 'FunName: update')");
    cw.write("@RequestMapping(value = '/%s/{%s}', method = RequestMethod.PUT)",
             source.entityVar, primaryKey);
    cw.write("public ApiResult update(");
    if (source.security) {
      cw.write(2, "@AuthenticationPrincipal RedisRememberMeService.User user,");
    }

    for (FieldDesc f : entityDesc.fields) {
      if (f.name.equals(primaryKey) || (f.timestamp && f.autoUpdate) ||
          f.isImmut || f.isUid || f.isDelay || f.isInternal) continue;

      cw.write(2, "@ApiQueryParam(name = '%s', description = '%s', required = false)",
               f.name, f.name);

      if (f.type.equals("LocalDate")) {
        cw.write(2, "@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)")
          .write(2, "@RequestParam Optional<LocalDate> %s,", f.name);
      } else if (f.type.equals("byte[]")) {
        cw.write(2, "@RequestParam MultipartFile %s,", f.name);
      } else if (f.isEnum) {
        cw.write(2, "@RequestParam Optional<%s.%s> %s,", source.entityClazz, f.type, f.name);
      } else {
        cw.write(2, "@RequestParam Optional<%s> %s,", box(f.type), f.name);
      }
      cw.newLine();
    }

    cw.write(2, "@ApiPathParam(name = '%s', description = '%s')", primaryKey, primaryKey)
      .write(2, "@PathVariable %s %s) %s{", entityDesc.primaryKeyType, primaryKey,
             entityDesc.hasBinary ? "throws Exception " : "")
      .newLine()
      .write(2, "%s %s = new %s();", source.entityClazz, source.entityVar, source.entityClazz);
               
    for (FieldDesc f : entityDesc.fields) {
      if ((f.timestamp && f.autoUpdate) ||
          f.isImmut || f.isUid || f.isDelay || f.isInternal) continue;
      
      if (f.name.equals(primaryKey)) {
        cw.write(2, "%s.set%s(%s);", source.entityVar, capitalize(f.name), f.name);
      } else if (f.type.equals("byte[]")) {
        cw.write(2, "%s.set%s(%s.getBytes());", source.entityVar, capitalize(f.name), f.name);
      } else if (f.type.equals("long") || f.type.equals("int")) {
        cw.write(2, "if (%s.isPresent()) %s.set%s(%s.get());",
                 f.name, source.entityVar, capitalize(f.name), f.name);
      } else {
        cw.write(2, "%s.set%s(%s.orElse(null));", source.entityVar, capitalize(f.name), f.name);
      }
    }
    cw.newLine()
      .write(2, "return %s.update(%s);", source.managerVar, source.entityVar)
      .write("}");

    return cw.toString(2);
  }

  String genControllerDeleteFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    CodeWriter cw = new CodeWriter();
    
    cw.write("@ApiMethod(description = 'FunName: delete %s by %s')", source.entityClazz, primaryKey)
      .write("@RequestMapping(value = '/%s/{%s}', method = RequestMethod.DELETE)",
             source.entityVar, primaryKey)
      .write("public ApiResult delete(");
    if (source.security) {
      cw.write(2, "@AuthenticationPrincipal RedisRememberMeService.User user,");
    }    
    cw.write(2, "@ApiPathParam(name = '%s', description = '%s')", primaryKey, primaryKey)
      .write(2, "@PathVariable %s %s) {", entityDesc.primaryKeyType, primaryKey)
      .write(2, "return %s.delete(%s);", source.managerVar, primaryKey)
      .write("}");
    
    return cw.toString(2);
  }
    
  String genControllerCode(EntitySource source, EntityDesc entityDesc) {
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
    if (entityDesc.hasBinary) {
      cw.write("import org.springframework.web.multipart.MultipartFile;");
    }
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
      .write("public class %s {", source.controllerClazz)
      .write(2, "@Autowired %s %s;", source.managerClazz, source.managerVar)
      .newLine();
    
    if (entityDesc.hasPrimaryKey) {
      cw.write(2, "private int pages;")
        .write(2, "private int count;")
        .newLine()
        .write(2, "@Autowired")
        .write(2, "public %s(Environment env) {", source.controllerClazz)
        .write(4, "this.pages = Integer.parseInt(env.getProperty('list.pages', '10'));")
        .write(4, "this.count = Integer.parseInt(env.getProperty('list.count', '20'));")
        .write(2, "}")
        .newLine();
    }

    cw.write(genControllerSelectFun(source, entityDesc))
      .write(genControllerInsertFun(source))
      .write(genControllerUpdateFun(source, entityDesc))
      .write(genControllerDeleteFun(source, entityDesc))
      .write("}");
    
    return cw.toString();
  }

  String genManagerClassName(String className) {
    return className + "Manager";
  }

  void genManagerPaging(CodeWriter cw, EntitySource source, EntityDesc entityDesc) {
    if (source.nopaging) return;
    String primaryKey = entityDesc.primaryKeyName;

    cw.write("Paging initPaging(int pages, int count) {")
      .write(2, "Paging paging = new Paging().setTable(%s.Sql.TABLE);", source.mapperClazz)
      .write(2, "paging.setRowId('%s').setCount(pages, count);", primaryKey)
      .write(2, "return paging;")
      .write("}")
      .newLine();
    
    cw.write("public ApiResult pages(%s %s, boolean backward, int pages, int count) {",
             entityDesc.primaryKeyType, primaryKey)
      .write(2, "Paging paging = initPaging(pages, count);")
      .newLine()
      .write(2, "List<%s> %ss;", box(entityDesc.primaryKeyType), source.entityVar)
      .write(2, "if (%s == -1) {", primaryKey)
      .write(4, "%ss = %s.first(paging);", source.entityVar, source.mapperVar)
      .write(2, "} else {")
      .write(4, "paging.setParams('%s', %s);", primaryKey, primaryKey)
      .write(4, "%ss = backward ? %s.backward(paging) : %s.forward(paging);",
             source.entityVar, source.mapperVar, source.mapperVar)
      .write(2, "}")
      .write(2, "return new ApiResult<List>(paging.pages(%ss, count));", source.entityVar)
      .write("}")
      .newLine();
    
    cw.write("public ApiResult list(Optional<%s> %s, int count) {",
             box(entityDesc.primaryKeyType), primaryKey)
      .write(2, "Paging paging = initPaging(1, count);")
      .newLine()
      .write(2, "List<%s> %ss;", source.entityClazz, source.entityVar)
      .write(2, "if (%s.isPresent()) paging.setParams('%s', %s.get());",
             primaryKey, primaryKey, primaryKey)
      .write(2, "%ss = %s.find(paging);", source.entityVar, source.mapperVar)
      .write(2, "return new ApiResult<List>(%ss);", source.entityVar)
      .write("}")
      .newLine();
  }

  String genManagerSelectFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    CodeWriter cw = new CodeWriter();

    genManagerPaging(cw, source, entityDesc);

    cw.write("public ApiResult findBy%s(%s %s) {",
             capitalize(primaryKey), entityDesc.primaryKeyType, primaryKey)
      .write(2, "%s %s = %s.findBy%s(%s);", source.entityClazz, source.entityVar, source.mapperVar,
             capitalize(primaryKey), primaryKey)
      .write(2, "return new ApiResult<%s>(%s);", source.entityClazz, source.entityVar)
      .write("}");

    return cw.toString(2);
  }

  String genManagerInsertFun(EntitySource source) {
    CodeWriter cw = new CodeWriter();

    if (!source.nontrans) cw.write("@Transactional");
    cw.write("public ApiResult add(%s %s) {", source.entityClazz, source.entityVar)
      .write(2, "%s.add(%s);", source.mapperVar, source.entityVar)
      .write(2, "return new ApiResult<%s>(%s);", source.entityClazz, source.entityVar)
      .write("}");
      
    return cw.toString(2);
  }

  String genManagerUpdateFun(EntitySource source) {
    CodeWriter cw = new CodeWriter();

    if (!source.nontrans) cw.write("@Transactional");
    cw.write("public ApiResult update(%s %s) {", source.entityClazz, source.entityVar)
      .write(2, "%s.update(%s);", source.mapperVar, source.entityVar)
      .write(2, "return ApiResult.ok();")
      .write("}");

    return cw.toString(2); 
  }

  String genManagerDeleteFun(EntitySource source, EntityDesc entityDesc) {
    if (!entityDesc.hasPrimaryKey) return null;
    String primaryKey = entityDesc.primaryKeyName;
    CodeWriter cw = new CodeWriter();

    if (!source.nontrans) cw.write("@Transactional");
    cw.write("public ApiResult delete(%s %s) {", entityDesc.primaryKeyType, primaryKey)
      .write(2, "%s.delete(%s);", source.mapperVar, primaryKey)
      .write(2, "return ApiResult.ok();")
      .write("}");

    return cw.toString(2); 
  }  
      
  String genManagerCode(EntitySource source, EntityDesc entityDesc) {
    CodeWriter cw = new CodeWriter();

    cw.write("package %s.manager;", source.packagePrefix)
      .newLine()
      .write("import java.util.*;")
      .write("import org.springframework.beans.factory.annotation.Autowired;")
      .write("import org.springframework.stereotype.Component;");
    if (!source.nontrans) cw.write("import org.springframework.transaction.annotation.*;");
    
    if (entityDesc.hasPrimaryKey && !source.nopaging) {
      cw.write("import commons.mybatis.Paging;");
    }
    
    cw.write("import %s.model.*;", source.packagePrefix)
      .write("import %s.mapper.*;", source.packagePrefix)
      .write("import %s.entity.*;", source.packagePrefix)
      .newLine();

    cw.write("@Component");
    cw.write("public class %s {", source.managerClazz)
      .write(2, "@Autowired %s %s;", source.mapperClazz, source.mapperVar)
      .newLine()
      .write(genManagerSelectFun(source, entityDesc))
      .write(genManagerInsertFun(source))
      .write(genManagerUpdateFun(source))
      .write(genManagerDeleteFun(source, entityDesc))
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
    
    @ApiQueryParam(name = "className", description = "class name tips")
    @RequestParam Optional<String> className,

    @ApiQueryParam(name = "packagePrefix", description = "package name prefix")
    @RequestParam Optional<String> packagePrefix,

    @ApiQueryParam(name = "security", description = "security flag, default yes")
    @RequestParam Optional<Boolean> security,
    
    @ApiQueryParam(name = "nontrans", description = "without transaction, default no")
    @RequestParam Optional<Boolean> nontrans,
    
    @ApiQueryParam(name = "nopaging", description = "without paging, default no")
    @RequestParam Optional<Boolean> nopaging,
    @ApiQueryParam(name = "nopublic", description = "without controller/manager, default no")
    @RequestParam Optional<Boolean> nopublic) {

    String parts[] = table.split("\\.");
    if (parts.length != 2) {
      System.out.println(parts.length);
      throw new RuntimeException("table must prefixed with dbname");
    }
    String shortTable = parts[1];

    EntitySource source = new EntitySource();
    source.dbHostPort    = dbHostPort;
    source.dbUser        = dbUser;
    source.dbPassword    = dbPassword;
    source.table         = table;
    source.shortTable    = shortTable;
    source.className     = capitalize(className.orElse(shortTable));
    source.packagePrefix = packagePrefix.orElse("example");
    source.security      = security.orElse(true);
    source.nontrans      = nontrans.orElse(false);
    source.nopaging      = nopaging.orElse(false);
    source.nopublic      = nopublic.orElse(false);

    source.entityClazz     = genEntityClassName(source.className);
    source.entityVar       = uncapitalize(source.entityClazz);
    source.mapperClazz     = genMapperClassName(source.className);
    source.mapperVar       = uncapitalize(source.mapperClazz);
    source.managerClazz    = genManagerClassName(source.className);
    source.managerVar      = uncapitalize(source.managerClazz);
    source.controllerClazz = genControllerClassName(source.className);

    String type = typeOpt.orElse("");
    if (type.equals("mapper")) {
      return genMapperCode(source, EntityDesc.buildFromTable(source));
    } else if (type.equals("entity")) {
      return genEntityCode(source, EntityDesc.buildFromTable(source));
    } else if (type.equals("api")) {
      return genControllerCode(source, EntityDesc.buildFromTable(source));
    } else if (type.equals("manager")) {
      return genManagerCode(source, EntityDesc.buildFromTable(source));
    } else if (type.equals("meta")) {
      return genMeta(EntityDesc.buildFromTable(source));
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

    if (source.nopublic) return builder.toString();

    builder.append("== BEGIN ").append(genControllerClassName(source.className)).append(" ==\n");
    builder.append(genControllerCode(source, desc));
    builder.append("== END ").append(genControllerClassName(source.className)).append(" ==\n");

    builder.append("== BEGIN ").append(genManagerClassName(source.className)).append(" ==\n");
    builder.append(genManagerCode(source, desc));
    builder.append("== END ").append(genManagerClassName(source.className)).append(" ==\n");

    return builder.toString();
  }

  private String boolToString(boolean b) {
    return b ? "TRUE\n" : "FALSE\n";
  }

  public String genMeta(EntityDesc entityDesc) {
    StringBuilder builder = new StringBuilder();
    for (FieldDesc f : entityDesc.fields) {
      builder.append("FieldDesc\n");
      builder.append("  name:       ").append(f.name).append("\n");
      builder.append("  type:       ").append(f.type).append("\n");
      builder.append("  size:       ").append(f.size).append("\n");
      builder.append("  isEnum:     ").append(boolToString(f.isEnum));
      builder.append("  isImmut:    ").append(boolToString(f.isImmut));
      builder.append("  isUid:      ").append(boolToString(f.isUid));
      builder.append("  isDelay:    ").append(boolToString(f.isDelay));
      builder.append("  isInternal: ").append(boolToString(f.isInternal));
      builder.append("--\n");
    }
    return builder.toString();
  }

  @ApiMethod(description = "AutoCode Help")  
  @RequestMapping(value = "/help", method = RequestMethod.GET, produces = "text/plain")
  public String help() {
    StringBuilder builder = new StringBuilder();
    builder.append("Example Table: autoCode\n");
    builder.append("  CREATE TABLE autoCode (\n");
    builder.append("    codeId    INT AUTO_INCREMENT PRIMARY KEY,\n");
    builder.append("    projectId INT COMMENT 'immut',\n");
    builder.append("    language  TINYINT COMMENT 'enum',\n");
    builder.append("    compiler  TINYINT COMMENT 'class#Compiler,internal',\n");
    builder.append("    authorId  INT COMMENT 'uid',\n");
    builder.append("    author    VARCHAR(32) NOT NULL,\n");
    builder.append("    email     VARCHAR(32) COMMENT 'EN',\n");
    builder.append("    reviewer  VARCHAR(32) COMMENT 'EN,delay',\n");
    builder.append("    linage    INT,\n");
    builder.append("    content   TEXT,\n");
    builder.append("    createAt  TIMESTAMP NOT NULL DEFAULT 0 COMMENT 'immut',\n");
    builder.append("    updateAt  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n");
    builder.append("    expireAt  DATE,\n");
    builder.append("    INDEX(author)\n");
    builder.append("  );\n\n");

    builder.append("If TINYINT with comment 'enum', it will be treat as enum type, and define a ENUM\n");
    builder.append("If TINYINT with comment 'class#Type', it will be treat as class type, and the type is Type\n");
    builder.append("If VARCHAR(N)/CHAR(N) with comment 'EN', it @Max will be N, or N/2\n");
    builder.append("If with comment 'immut', it will only not be updated.\n");
    builder.append("If with comment 'uid', it will be set from User, 'uid' will always be 'immut'\n");
    builder.append("If with comment 'delay', it will be updated after insert.\n");
    builder.append("If with comment 'internal', it will not be update from outside.\n");
    builder.append("comment can be combined with comma(,), example 'EN,delay'\n");
    builder.append("\n");

    builder.append("API options\n");
    builder.append("  dbHostPort    required\n");
    builder.append("  dbUser        required\n");
    builder.append("  dbPassword    required\n");
    builder.append("  table         optional format(db.table)\n");
    builder.append("  className     optional default use table's name\n");
    builder.append("  packagePrefix optional format(com.example) default example\n");
    builder.append("  security      optional format(yes|no) default yes. if yes, add @AuthenticationPrincipal\n");
    builder.append("  nontrans      optional format(yes|no) default no. if yes, without @@Transactional\n");
    builder.append("  nopaging      optional format(yes|no) default no. if yes, without Paging\n");
    builder.append("  nopublic      optional format(yes|no) default no. if yes, without controller and manager\n");

    return builder.toString();
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus
  public String internalServerError(Exception e) {
    e.printStackTrace();
    return e.toString();
  }
}
