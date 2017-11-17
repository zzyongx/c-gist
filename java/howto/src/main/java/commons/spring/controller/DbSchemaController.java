package commons.spring.controller;

import java.sql.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired Environment env;

  private List<DbInfo> getDbs(String name) {
    List<DbInfo> dbs = new ArrayList<>();

    String defUserName = env.getProperty(name == null ? "jdbc.username" : name + ".jdbc.username");
    String defPassword = env.getProperty(name == null ? "jdbc.password" : name + ".jdbc.password");

    for (String suffix : EnvHelper.getPropertyNameSuffixWithPrefix(env, name == null ? "jdbc.url" : name + ".jdbc.url")) {
      DbInfo db = new DbInfo();

      if (suffix.isEmpty()) {
        db.url = env.getProperty(name == null ? "jdbc.url" : name + ".jdbc.url");
      } else {
        db.url = env.getProperty(name == null ? "jdbc.url." + suffix : name + ".jdbc.url." + suffix);
        db.username = env.getProperty(name == null ? "jdbc.username." + suffix : name + ".jdbc.username." + suffix);
        db.password = env.getProperty(name == null ? "jdbc.password." + suffix : name + ".jdbc.password." + suffix);
      }

      if (db.username == null) db.username = defUserName;
      if (db.password == null) db.password = defPassword;

      if (db.url != null) dbs.add(db);
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

  private Map<String, Object> getDbSchema(String configName, String dbName) {
    Map<String, Object> tableMap = new HashMap<>();

    for (DbInfo info : getDbs(configName)) {
      SingleConnectionDataSource c = new SingleConnectionDataSource(
        info.url, info.username, info.password, false);

      if (dbName == null) {
        int slashIndex = info.url.lastIndexOf('/');
        if (slashIndex > 0 && slashIndex+1 != info.url.length()) {
          int questionIndex = info.url.lastIndexOf('?');
          dbName  = slashIndex < questionIndex ? info.url.substring(slashIndex+1, questionIndex) :
            info.url.substring(slashIndex+1);
        } else {
          continue;
        }
      }

      try {
        JdbcTemplate jdbc = new JdbcTemplate(c);

        for (String table : showTables(jdbc, dbName)) {
          tableMap.put(table, showTableSchema(jdbc, dbName, table));
        }
      } finally {
        c.destroy();
      }
    }

    return tableMap;
  }

  @RequestMapping(value = "/dbschema", method = RequestMethod.GET)
  public Map<String, Object> getDbSchema(
    @RequestParam Optional<String> configName,
    @RequestParam Optional<String> dbName) {
    return getDbSchema(configName.orElse(null), dbName.orElse(null));
  }

  private List<String> diffTable(String table, TableSchema expect, TableSchema actual) {
    List<String> diffs = new ArrayList<>();
    Map<String, TableField> expectFields = expect.getFields();
    Map<String, TableField> actualFields = actual.getFields();

    for (Map.Entry<String, TableField> entry : expectFields.entrySet()) {
      TableField expectField = entry.getValue();
      TableField actualField = actualFields.get(entry.getKey());
      String field = "field " + table + "." + entry.getKey();

      if (actualField == null) {
        diffs.add(field + " not found");
      } else {
        if (!expectField.type.equals(actualField.type)) diffs.add(field + " type not match");
        if (!expectField.key.equals(actualField.key)) diffs.add(field + " key not match");
        if (!expectField.def.equals(actualField.def)) diffs.add(field + " default not match");
        if (!expectField.extra.equals(actualField.extra)) diffs.add(field + " extra not match");
      }
    }

    List<TableIndex> expectIndexs = expect.getIndexs();
    List<TableIndex> actualIndexs = actual.getIndexs();

    for (TableIndex expectIndex : expectIndexs) {
      boolean found = false;
      for (TableIndex actualIndex : actualIndexs) {
        if (expectIndex.equals(actualIndex)) found = true;
      }
      if (!found) diffs.add("index " + table + "." + expectIndex.toString() + " not found");
    }

    return diffs;
  }

  @RequestMapping(value = "/dbschema/diff", method = RequestMethod.PUT)
  public String diffDbSchema(@RequestParam Optional<String> configName,
                             @RequestParam Optional<String> dbName,
                             @RequestParam Optional<List<String>> ignoreTables,
                             @RequestBody String body) {
    List<String> diffs = new ArrayList<>();

    Map<String, TableSchema> expectMap = JsonHelper.readValue(
      body, new TypeReference<Map<String, TableSchema>>(){});

    Map<String, Object> actualMap = getDbSchema(configName.orElse(null), dbName.orElse(null));

    for (Map.Entry<String, TableSchema> entry : expectMap.entrySet()) {
      if (ignoreTables.isPresent() && ignoreTables.get().indexOf(entry.getKey()) >= 0) continue;

      @SuppressWarnings("unchecked")
      TableSchema actualSchema = (TableSchema) actualMap.get(entry.getKey());

      if (actualSchema == null) diffs.add("table " + entry.getKey() + " not found");
      else diffs.addAll(diffTable(entry.getKey(), entry.getValue(), actualSchema));
    }

    return String.join("\n", diffs);
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus
  public String internalServerError(Exception e) {
    e.printStackTrace();
    return e.toString();
  }
}
