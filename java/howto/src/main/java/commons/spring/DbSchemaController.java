package commons.spring;

import java.sql.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.web.bind.annotation.*;
import commons.utils.JsonHelper;
import commons.utils.EnvHelper;

@RestController
@RequestMapping("/api")
public class DbSchemaController {
  static class DbInfo {
    public String url;
    public String username;
    public String password;
  }

  static class TableField {
    public String name;
    public String type;
    public String key;
    public String def;
    public String extra;
  }

  static class TableIndex {
    public List<String> fields = new ArrayList<>();
    public boolean uniq = false;

    public void addField(String field) {
      fields.add(field);
    }
    
    public void setUniq(boolean uniq) {
      this.uniq = uniq;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof TableIndex) {
        TableIndex index = (TableIndex) o;
        return index.uniq == uniq && index.fields.equals(fields);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      throw new RuntimeException("not implemented");
    }

    @Override
    public String toString() {
      return "unique: " + uniq + "; fields:" + fields.toString();
    }
  }

  static class TableSchema {
    Map<String, TableField> fields = new HashMap<>();
    List<TableIndex> indexs = new ArrayList<>();
    
    public void addField(String name, TableField field) {
      if (field.def == null) field.def = "";
      fields.put(name, field);
    }
    public Map<String, TableField> getFields() {
      return fields;
    }

    public void addIndex(TableIndex index) {
      indexs.add(index);
    }
    public List<TableIndex> getIndexs() {
      return indexs;
    }
  }
  
  List<DbInfo> dbs = new ArrayList<>();

  public DbSchemaController(Environment env) {
    String defUserName = env.getProperty("jdbc.username");
    String defPassword = env.getProperty("jdbc.password");
    
    for (String suffix : EnvHelper.getPropertyNameSuffixWithPrefix(env, "jdbc.url")) {
      DbInfo db = new DbInfo();
      
      if (suffix.isEmpty()) {
        db.url = env.getProperty("jdbc.url");
      } else {
        db.url = env.getProperty("jdbc.url." + suffix);
        db.username = env.getProperty("jdbc.username." + suffix);
        db.password = env.getProperty("jdbc.password." + suffix);
      }

      if (db.username == null) db.username = defUserName;
      if (db.password == null) db.password = defPassword;

      if (db.url != null) dbs.add(db);
    }
  }

  List<String> showDatabases(JdbcTemplate jdbc) {
    List<String> dbs = new ArrayList<>();
    for (Map<String, Object> map : jdbc.queryForList("SHOW DATABASES")) {
      String db = (String) map.get("Database");
      if (!db.equals("test") && !db.equals("information_schema")) {
        dbs.add(db);
      }
    }
    return dbs;
  }

  List<String> showTables(JdbcTemplate jdbc, String db) {
    List<String> tables = new ArrayList<>();
    for (Map<String, Object> map : jdbc.queryForList("SHOW TABLES IN " + db)) {
      String table = (String) map.get("Tables_in_" + db);
      tables.add(table);
    }
    return tables;
  }

  TableSchema showTableSchema(JdbcTemplate jdbc, String db, String table) {
    String fullTableName = db + "." + table;
    TableSchema schema = new TableSchema();    

    for (Map<String, Object> map : jdbc.queryForList("DESC " + fullTableName)) {
      TableField field = new TableField();
      field.name  = (String) map.get("Field");
      field.type  = (String) map.get("Type");
      field.key   = (String) map.get("Key");
      field.def   = (String) map.get("Default");
      field.extra = (String) map.get("extra");
      schema.addField(field.name, field);
    }

    String lastName = null;
    TableIndex index = null;
    for (Map<String, Object> map : jdbc.queryForList("SHOW INDEX IN " + fullTableName)) {
      String name = (String) map.get("Key_name");
      String field = (String) map.get("Column_name");
      
      if (name.equals(lastName)) {
        index.addField(field);
      } else {
        if (index != null) schema.addIndex(index);
        
        index = new TableIndex();
        index.addField(field);

        long nonUnique = (Long) map.get("Non_unique");
        index.setUniq(nonUnique == 0);
      }
      lastName = name;
    }
    if (index != null) schema.addIndex(index);

    return schema;
  }
  
  @RequestMapping(value = "/dbschema", method = RequestMethod.GET)
  public Map<String, Object> getDbSchema() {
    Map<String, Object> dbMap = new HashMap<>();
    
    for (DbInfo info : dbs) {
      SingleConnectionDataSource c = new SingleConnectionDataSource(
        info.url, info.username, info.password, false);
      try {
        JdbcTemplate jdbc = new JdbcTemplate(c);
        for (String db : showDatabases(jdbc)) {
          Map<String, Object> tableMap = new HashMap<>();
          dbMap.put(db.replaceAll("_test$", ""), tableMap);
          
          for (String table : showTables(jdbc, db)) {
            tableMap.put(table, showTableSchema(jdbc, db, table));
          }
        }
      } finally {
        c.destroy();
      }
    }
    
    return dbMap;
  }

  String diffDb(String db, Map<String, TableSchema> expect, Map<String, TableSchema> actual) {
    for (Map.Entry<String, TableSchema> entry : expect.entrySet()) {
      TableSchema expectSchema = entry.getValue();
      TableSchema actualSchema = (TableSchema) actual.get(entry.getKey());
      if (actualSchema == null) {
        return "table " + db + "." + entry.getKey() + " not found";
      }

      String diff = diffTable(db, entry.getKey(), expectSchema, actualSchema);
      if (diff != null) return diff;
    }
    return null;
  }

  String diffTable(String db, String table, TableSchema expect, TableSchema actual) {
    Map<String, TableField> expectFields = expect.getFields();
    Map<String, TableField> actualFields = actual.getFields();

    for (Map.Entry<String, TableField> entry : expectFields.entrySet()) {
      TableField expectField = entry.getValue();
      TableField actualField = actualFields.get(entry.getKey());
      String field = "field " + db + "." + table + "." + entry.getKey();
      
      if (actualField == null) return field + " not found";
      if (!expectField.type.equals(actualField.type)) return field + " type not match";
      if (!expectField.key.equals(actualField.key)) return field + " key not match";
      if (!expectField.def.equals(actualField.def)) return field + " default not match";
      if (!expectField.extra.equals(actualField.extra)) return field + " extra not match";
    }

    List<TableIndex> expectIndexs = expect.getIndexs();
    List<TableIndex> actualIndexs = actual.getIndexs();

    for (TableIndex expectIndex : expectIndexs) {
      boolean found = false;
      for (TableIndex actualIndex : actualIndexs) {
        if (expectIndex.equals(actualIndex)) found = true;
      }
      if (!found) return "index " + db + "." + table + "." + expectIndex.toString() + " not found";
    }

    return null;    
  }

  @RequestMapping(value = "/dbschema/diff", method = RequestMethod.PUT)
  public String diffDbSchema(@RequestBody String body) {
    Map<String, Map<String, TableSchema>> expectMap = JsonHelper.readValue(
      body, new TypeReference<Map<String, Map<String, TableSchema>>>(){});

    Map<String, Object> actualMap = getDbSchema();

    for (Map.Entry<String, Map<String, TableSchema>> entry : expectMap.entrySet()) {
      Map<String, TableSchema> expectDbMap = entry.getValue();
      @SuppressWarnings("unchecked")
      Map<String, TableSchema> actualDbMap = (Map<String, TableSchema>)
        actualMap.get(entry.getKey());
      if (actualDbMap == null) return "db " + entry.getKey() + " not found";

      String diff = diffDb(entry.getKey(), expectDbMap, actualDbMap);
      if (diff != null) return diff;
    }

    return "";
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus
  public String internalServerError(Exception e) {
    e.printStackTrace();
    return e.toString();
  }
}
