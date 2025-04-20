package com.palisand.bones.persist;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.palisand.bones.persist.CommandScheme.Metadata.DbIndex;
import com.palisand.bones.persist.CommandScheme.Metadata.DbTable;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbAttribute;
import com.palisand.bones.persist.Database.DbClass.DbRole;
import com.palisand.bones.persist.Database.DbClass.DbSearchMethod;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
public class CommandScheme extends HashMap<String,Class<?>> {
  private Map<Class<?>,JDBCType> typeMap = new HashMap<>();
  protected static final String FOREIGN_KEY_PREFIX = "fk_";
  protected Map<DbClass,Statements> statements = new HashMap<>();
  private Consumer<String> logger = null;
  
  @Getter
  @Setter
  static private class Statements {
    private PreparedStatement insert;
    private String insertSql;
    private PreparedStatement update;
    private String updateSql;
    private PreparedStatement delete;
    private String deleteSql;
    private PreparedStatement selectOne;
    private String selectOneSql;
  }
  
  @Data
  static class Metadata {
    
    @Data
    static class DbTable {
      private final String name;
      private final Map<String,DbColumn> fields = new TreeMap<>();
      private DbIndex primaryKey;
      private final Map<String,DbIndex> foreignKeys = new TreeMap<>();
      private final Map<String,DbIndex> indices = new TreeMap<>();
    }
    
    @Data 
    static class DbColumn {
      private final String name;
      private final JDBCType type;
      private final boolean nullable;
    }
    
    @Data 
    static class DbIndex {
      private final String name;
      private final boolean unique;
      private DbTable references = null;
      private final List<DbColumn> fields = new ArrayList<>();
    }

    private final Map<String,DbTable> tables = new TreeMap<>();
    private String catalog;
  }
  
  Metadata getMetadata(Connection connection) throws SQLException {
    DatabaseMetaData dbmd = connection.getMetaData();
    Metadata metadata = new Metadata();
    ResultSet tables = dbmd.getTables(connection.getCatalog(),null,null,new String[] { "TABLE" });
    List<Object[]> tempFks = new ArrayList<>();
    while (tables.next()) {
      DbTable table = new DbTable(tables.getString("TABLE_NAME").toLowerCase());
      metadata.getTables().put(table.getName(),table);
      ResultSet fields = dbmd.getColumns(connection.getCatalog(),null,table.getName(),null);
      while (fields.next()) {
        Metadata.DbColumn field = new Metadata.DbColumn(fields.getString("COLUMN_NAME").toLowerCase(),
            JDBCType.valueOf(fields.getInt("DATA_TYPE")),fields.getBoolean("IS_NULLABLE"));
        table.getFields().put(field.getName(),field);
      }
      ResultSet pkeys = dbmd.getPrimaryKeys(connection.getCatalog(),null,table.getName());
      DbIndex pkey = new DbIndex("pk",true);
      table.setPrimaryKey(pkey);
      while (pkeys.next()) {
        pkey.getFields().add(table.getFields().get(pkeys.getString("COLUMN_NAME").toLowerCase()));
      }
      ResultSet fkeys = dbmd.getCrossReference(connection.getCatalog(),null,null,connection.getCatalog(),null,table.getName());
      Metadata.DbIndex fkey = null;
      while (fkeys.next()) {
        String name = fkeys.getString("FK_NAME").toLowerCase();
        if (fkey == null || fkey.getName().equals(name)) {
          fkey = new DbIndex(name,false);
          table.getForeignKeys().put(name,fkey);
          tempFks.add(new Object[] {fkey,fkeys.getString("PKTABLE_NAME").toLowerCase()});
        }
        fkey.getFields().add(table.getFields().get(fkeys.getString("FKCOLUMN_NAME").toLowerCase()));
      }
      ResultSet indices = dbmd.getIndexInfo(connection.getCatalog(),null,table.getName(),false,false);
      DbIndex index = null;
      while (indices.next()) {
        String name = indices.getString("INDEX_NAME").toLowerCase();
        if (index == null || !index.getName().equalsIgnoreCase(name)) {
          index = new DbIndex(name,!indices.getBoolean("NON_UNIQUE"));
          table.getIndices().put(name,index);
        }
        index.getFields().add(table.getFields().get(indices.getString("COLUMN_NAME").toLowerCase()));
      }
      String key = null;
      for (Entry<String,DbIndex> entry: table.getIndices().entrySet()) {
        if (entry.getValue().getFields().equals(table.getPrimaryKey().getFields())) {
          key = entry.getKey();
          break;
        }
      }
      if (key != null) {
        table.getIndices().remove(key);
      }
    }
    for (Object[] refs: tempFks) {
      DbIndex index = (DbIndex)refs[0];
      DbTable table = metadata.getTables().get(refs[1]);
      index.setReferences(table);
    }
    return metadata;
  }
  
  
  private static class Comma {
    boolean first = true;
    
    public void next(StringBuilder sb) {
      if (first) {
        first = false;
      } else {
        sb.append(",");
      }
    }
  }
  
  public CommandScheme() {
    typeMap.put(short.class,JDBCType.SMALLINT);
    typeMap.put(Short.class,JDBCType.SMALLINT);
    typeMap.put(int.class,JDBCType.INTEGER);
    typeMap.put(Integer.class,JDBCType.INTEGER);
    typeMap.put(long.class,JDBCType.BIGINT);
    typeMap.put(Long.class,JDBCType.BIGINT);
    typeMap.put(boolean.class,JDBCType.BOOLEAN);
    typeMap.put(Boolean.class,JDBCType.BOOLEAN);
    typeMap.put(String.class,JDBCType.VARCHAR);
    typeMap.put(OffsetDateTime.class,JDBCType.TIME_WITH_TIMEZONE);
    typeMap.put(LocalDate.class,JDBCType.DATE);
    typeMap.put(BigDecimal.class,JDBCType.DECIMAL);
    typeMap.put(double.class,JDBCType.DOUBLE);
    typeMap.put(Double.class,JDBCType.DOUBLE);
    typeMap.put(float.class,JDBCType.REAL);
    typeMap.put(Float.class,JDBCType.REAL);
  }
  
  public CommandScheme logger(Consumer<String> logger) {
    this.logger = logger;
    return this;
  }
  
  protected void log(String str) {
    this.logger.accept(str);
  }
  
  protected String typeName(JDBCType type) {
    return type.getName();
  }
  
  protected boolean execute(Connection connection, String sql) throws SQLException {
    log(sql);
    return connection.createStatement().execute(sql);
  }
  
  private JDBCType getJDBCType(Class<?> cls) throws SQLException {
    JDBCType type = typeMap.get(cls);
    if (type == null) {
      throw new SQLException("Type " + cls + " is not (yet) supported");
    }
    return type;
  }
  
  private String getType(DbAttribute attribute) throws SQLException {
    JDBCType type = getJDBCType(attribute.getType());
    return typeName(type);
  }
  
  private void appendColumn(StringBuilder sql, String prefix, DbAttribute attribute, boolean nullable) throws SQLException {
    if (!prefix.isEmpty()) {
      sql.append(prefix);
      sql.append('_');
    }
    appendColumn(sql,attribute,nullable);
  }
  
  private void appendColumn(StringBuilder sql, DbAttribute attribute, boolean nullable) throws SQLException {
    sql.append(attribute.getName());
    sql.append(" ");
    sql.append(getType(attribute));
    if (!nullable) {
      sql.append(" NOT");
    }
    sql.append(" NULL");
  }
  
  protected void upgradeColumns(Connection connection,DbTable dbTable, DbClass entity) throws SQLException {
    TreeSet<String> fieldsToRemove = new TreeSet<String>(
        dbTable.getFields().keySet().stream().map(name -> name.toLowerCase()).toList());
    for (DbAttribute attribute: entity.getFields()) {
      String name = attribute.getName().toLowerCase();
      if (!fieldsToRemove.remove(name)) {
        addFieldToTable(connection, entity, attribute);
      }
    }
    for (DbRole role: entity.getForeignKeys()) {
      for (DbAttribute attribute: role.getForeignKey().getFields()) {
        String name = attribute.getName().toLowerCase();
        if (!fieldsToRemove.remove(name)) {
          addFieldToTable(connection,entity,attribute);
        }
      }
    }
    for (String name: fieldsToRemove) {
      removeFieldFromTable(connection,entity,name);
    }
  }
  
  protected void upgradeIndices(Connection connection,DbTable dbTable, DbClass entity) throws SQLException {
    TreeSet<String> indicesToRemove = new TreeSet<String>(
        Stream.concat(
            dbTable.getIndices().keySet().stream(),
            dbTable.getForeignKeys().keySet().stream()).map(name -> name.toLowerCase()).toList());
    if (entity.getSuperClass() != null) {
      String name = FOREIGN_KEY_PREFIX + entity.getName() + "_parent";
      if (!indicesToRemove.remove(name)) {
        createParentKey(connection,entity);
      }
    }
    for (DbSearchMethod index: entity.getIndices().values()) {
      String name = index.getName().toLowerCase();
      if (!indicesToRemove.remove(name)) {
        createIndex(connection, index);
      }
    }
    for (DbRole role: entity.getForeignKeys()) {
      DbSearchMethod index = role.getForeignKey();
      String name = index.getName().toLowerCase();
      if (!indicesToRemove.remove(name)) {
        createIndex(connection, index);
      }
    }
    for (String name: indicesToRemove) {
      dropContraint(connection,entity.getName(),name);
      dropIndex(connection,entity,name);
    }
  }
  
  protected void upgradeTable(Connection connection, DbTable dbTable, DbClass entity) throws SQLException {
    if (dbTable == null) {
      createTable(connection,entity);
    } else {
      upgradeColumns(connection,dbTable,entity);
      upgradeIndices(connection,dbTable,entity);
    }
  }
  
  public void upgradeForeignKey(Connection connection, DbTable table, DbRole role) throws SQLException {
    DbIndex fk = null;
    if (table != null) {
      fk = table.getForeignKeys().get(role.getForeignKey().getName());
    }
    if (fk == null) {
      createForeignKey(connection,role);
      createIndex(connection,role.getForeignKey());
    }
  }
  
  protected void removeFieldFromTable(Connection connection, DbClass entity, String columnName) throws SQLException {
    StringBuilder sql = new StringBuilder("ALTER TABLE ");
    sql.append(entity.getName());
    sql.append(" DROP COLUMN ");
    sql.append(columnName);
    execute(connection,sql.toString());
  }
  
  protected void addFieldToTable(Connection connection, DbClass entity, DbAttribute attribute) throws SQLException {
    StringBuilder sql = new StringBuilder("ALTER TABLE ");
    sql.append(entity.getName());
    sql.append(" ADD ");
    appendColumn(sql,attribute,attribute.isNullable());
    execute(connection,sql.toString());
  }
  
  public void createTable(Connection connection, DbClass entity) throws SQLException {
    StringBuilder sql = new StringBuilder("CREATE TABLE ");
    sql.append(entity.getName());
    sql.append("(");
    for (DbAttribute attribute: entity.getFields()) {
      appendColumn(sql,attribute,attribute.isNullable());
      if (attribute.getId() != null && attribute.getId().generated() && entity.getSuperClass() == null) {
        sql.append(" GENERATED ALWAYS AS IDENTITY");
      }
      sql.append(",");
    }
    for (DbRole role: entity.getForeignKeys()) {
      for (DbAttribute field: role.getForeignKey().getFields()) {
        appendColumn(sql,field,true);
        sql.append(",");
      }
    }
    sql.append("PRIMARY KEY(");
    sql.append(entity.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append("))");
    execute(connection, sql.toString());
    for (DbSearchMethod method: entity.getIndices().values()) {
      createIndex(connection,method);
    }
  }
  
  public String createLinkTable(Connection connection, DbTable table, DbRole first) throws SQLException {
    DbRole second = first.getOpposite();
    String name = null;
    if (second != null) {
      StringBuilder sql = new StringBuilder("CREATE TABLE ");
      name = first.getTablename();
      sql.append(name);
      sql.append("(");
      DbClass fk = Database.getDbClass(first.getType());
      Comma sqlComma = new Comma();
      Comma pkComma = new Comma();
      Comma ffkComma = new Comma();
      StringBuilder pk = new StringBuilder();
      StringBuilder ffk = new StringBuilder();
      for (DbAttribute pkf: fk.getPrimaryKey().getFields()) {
        sqlComma.next(sql);
        pkComma.next(pk);
        ffkComma.next(ffk);
        appendColumn(sql,first.getName(),pkf,false);
        pk.append(first.getName() + '_' + pkf.getName());
        ffk.append(first.getName() + '_' + pkf.getName());
      }
      DbClass sk = Database.getDbClass(second.getType());
      Comma sfkComma = new Comma();
      StringBuilder sfk = new StringBuilder();
      for (DbAttribute pkf: sk.getPrimaryKey().getFields()) {
        sqlComma.next(sql);
        pkComma.next(pk);
        sfkComma.next(sfk);
        appendColumn(sql,second.getName(),pkf,false);
        pk.append(second.getName() + '_' + pkf.getName());
        sfk.append(second.getName() + '_' + pkf.getName());
      }
      sql.append(",PRIMARY KEY(");
      sql.append(pk.toString());
      sql.append("),CONSTRAINT ");
      sql.append(FOREIGN_KEY_PREFIX);
      sql.append(first.getTablename());
      sql.append('_');
      sql.append(first.getName());
      sql.append(" FOREIGN KEY(");
      sql.append(ffk.toString());
      sql.append(") REFERENCES ");
      sql.append(fk.getName());
      sql.append("(");
      sql.append(fk.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
      sql.append("),CONSTRAINT ");
      sql.append(FOREIGN_KEY_PREFIX);
      sql.append(first.getTablename());
      sql.append('_');
      sql.append(second.getName());
      sql.append(" FOREIGN KEY(");
      sql.append(sfk.toString());
      sql.append(") REFERENCES ");
      sql.append(sk.getName());
      sql.append("(");
      sql.append(sk.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
      sql.append("))");
      execute(connection,sql.toString());
    }
    return name;
  }
  
  public void dropTable(Connection connection, String name) throws SQLException {
    StringBuilder sql = new StringBuilder("DROP TABLE ");
    sql.append(name);
    execute(connection,sql.toString());
  }
  
  protected void dropIndex(Connection connection, DbClass entity, String indexName) throws SQLException {
    StringBuilder sql = new StringBuilder("DROP INDEX IF EXISTS ");
    sql.append(indexName);
    execute(connection, sql.toString());
  }

  public void dropContraint(Connection connection, String tableName, String constraintName) throws SQLException {
    StringBuilder sql = new StringBuilder("ALTER TABLE ");
    sql.append(tableName);
    sql.append(" DROP CONSTRAINT IF EXISTS ");
    sql.append(constraintName);
    execute(connection,sql.toString());
  }

  public void createForeignKey(Connection connection, DbRole role) throws SQLException {
    StringBuilder sql = new StringBuilder("ALTER TABLE ");
    sql.append(role.getEntity().getName());
    sql.append(" ADD CONSTRAINT ");
    sql.append(FOREIGN_KEY_PREFIX);
    DbClass fk = Database.getDbClass(role.getType());
    sql.append(role.getEntity().getName());
    sql.append('_');
    sql.append(role.getName());
    sql.append(" FOREIGN KEY(");
    sql.append(role.getForeignKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append(") REFERENCES ");
    sql.append(fk.getName());
    sql.append("(");
    sql.append(fk.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append(")");
    execute(connection,sql.toString());
  }
  
  protected Statements getInsertStatement(Connection connection, DbClass entity) throws SQLException {
    Statements stmts = statements.computeIfAbsent(entity,e -> new Statements());
    if (stmts.getInsert() == null) {
      StringBuilder sql = new StringBuilder("INSERT INTO ");
      StringBuilder params = new StringBuilder();
      sql.append(entity.getName());
      sql.append("(");
      Comma sqlComma = new Comma();
      Comma pComma = new Comma();
      for (DbAttribute field: entity.getFields()) {
        if (!field.isGenerated()) {
          sqlComma.next(sql);
          pComma.next(params);
          sql.append(field.getName());
          params.append("?");
        }
      }
      for (DbRole role: entity.getForeignKeys()) {
        DbClass other = Database.getDbClass(role.getType());
        for (DbAttribute field: other.getPrimaryKey().getFields()) {
          sqlComma.next(sql);
          pComma.next(params);
          sql.append(role.getName());
          sql.append('_');
          sql.append(field.getName());
          
          params.append("?");
        }
      }
      sql.append(") VALUES (");
      sql.append(params);
      sql.append(")");
      stmts.setInsert(connection.prepareStatement(sql.toString(),Statement.RETURN_GENERATED_KEYS));
      stmts.setInsertSql(sql.toString());
    }
    return stmts;
  }
  
  private void nextValue(StringBuilder sql, Object value) {
    if (sql != null) {
      int pos = sql.indexOf("?");
      sql.replace(pos,pos+1,value.toString());
    }
  }

  public void insert(Connection connection, DbClass entity, Object object) throws SQLException {
    if (entity.getSuperClass() != null) {
      insert(connection,entity.getSuperClass(),object);
    }
    Statements stmts = getInsertStatement(connection,entity);
    PreparedStatement stmt = stmts.getInsert();
    StringBuilder sql = null;
    if (logger != null) {
      sql = new StringBuilder(stmts.getInsertSql());
    }
    int index = 1;
    for (DbAttribute field: entity.getFields()) {
      if (!field.isGenerated()) {
        Object value = field.get(object);
        if (value != null) {
          stmt.setObject(index++,value);
          nextValue(sql,value);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
          nextValue(sql,"null");
        }
      }
    }
    for (DbRole role: entity.getForeignKeys()) {
      Object value = role.get(object);
      DbClass other = Database.getDbClass(role.getType());
      for (DbAttribute field: other.getPrimaryKey().getFields()) {
        if (value != null) {
          Object key =  field.get(value);
          stmt.setObject(index++,key);
          nextValue(sql,value);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
          nextValue(sql,"null");
        }
      }
    }
    if (logger != null) {
      logger.accept(sql.toString());
    }
    if (stmt.execute()) {
      ResultSet keys = stmt.getResultSet();
      if (keys.next()) {
        index = 1;
        for (DbAttribute field: entity.getPrimaryKey().getFields()) {
          if (field.isGenerated()) {
            field.set(object,keys.getObject(index++));
          }
        }
      }
    }
  }

  public String getDatabaseName(Connection connection) throws SQLException {
    return connection.getCatalog();
  }

  public void createIndex(Connection connection, DbSearchMethod path) throws SQLException {
    StringBuilder sql = new StringBuilder("CREATE ");
    if (path.isUnique()) {
      sql.append("UNIQUE ");
    }
    sql.append("INDEX IF NOT EXISTS ");
    sql.append(path.getName());
    sql.append(" ON ");
    sql.append(path.getEntity().getName());
    sql.append("(");
    Comma comma = new Comma();
    for (DbAttribute field: path.getFields()) {
      comma.next(sql);
      sql.append(field.getName());
    }
    sql.append(")");
    execute(connection,sql.toString());
  }

  protected void createParentKey(Connection connection, DbClass cls) throws SQLException {
    StringBuilder sql = new StringBuilder("ALTER TABLE ");
    sql.append(cls.getName());
    sql.append(" ADD CONSTRAINT ");
    sql.append(FOREIGN_KEY_PREFIX);
    sql.append(cls.getName());
    sql.append("_parent");
    sql.append(" FOREIGN KEY(");
    sql.append(cls.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append(") REFERENCES ");
    sql.append(cls.getSuperClass().getName());
    sql.append("(");
    sql.append(cls.getSuperClass().getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append(")");
    execute(connection,sql.toString());
  }
  
  public void upgradeParent(Connection connection, DbTable table, DbClass cls) throws SQLException {
    String name = FOREIGN_KEY_PREFIX + cls.getName() + "_parent";
    if (table == null || table.getForeignKeys().get(name) == null) {
      createParentKey(connection,cls);
    }
  }
 
}
