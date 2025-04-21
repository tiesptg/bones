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
import com.palisand.bones.persist.Database.DbClass.DbField;
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
      private final Map<String,DbColumn> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      private DbIndex primaryKey;
      private final Map<String,DbIndex> foreignKeys = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      private final Map<String,DbIndex> indices = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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

    private final Map<String,DbTable> tables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private String catalog;
  }
  
  Metadata getMetadata(Connection connection) throws SQLException {
    DatabaseMetaData dbmd = connection.getMetaData();
    Metadata metadata = new Metadata();
    ResultSet tables = dbmd.getTables(connection.getCatalog(),connection.getSchema(),null,new String[] { "TABLE" });
    while (tables.next()) {
      DbTable table = new DbTable(tables.getString("TABLE_NAME"));
      metadata.getTables().put(table.getName(),table);
      ResultSet fields = dbmd.getColumns(connection.getCatalog(),connection.getSchema(),table.getName(),null);
      while (fields.next()) {
        Metadata.DbColumn field = new Metadata.DbColumn(fields.getString("COLUMN_NAME"),
            JDBCType.valueOf(fields.getInt("DATA_TYPE")),fields.getBoolean("IS_NULLABLE"));
        table.getFields().put(field.getName(),field);
      }
      ResultSet pkeys = dbmd.getPrimaryKeys(connection.getCatalog(),connection.getSchema(),table.getName());
      DbIndex pkey = new DbIndex("pk",true);
      table.setPrimaryKey(pkey);
      while (pkeys.next()) {
        pkey.getFields().add(table.getFields().get(pkeys.getString("COLUMN_NAME")));
      }
      ResultSet indices = dbmd.getIndexInfo(connection.getCatalog(),connection.getSchema(),table.getName(),false,false);
      DbIndex index = null;
      while (indices.next()) {
        String name = indices.getString("INDEX_NAME");
        if (index == null || !index.getName().equalsIgnoreCase(name)) {
          index = new DbIndex(name,!indices.getBoolean("NON_UNIQUE"));
          table.getIndices().put(name,index);
        }
        index.getFields().add(table.getFields().get(indices.getString("COLUMN_NAME")));
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
    for (DbTable table: metadata.getTables().values()) {
    	for (DbTable other: metadata.getTables().values()) {
	      ResultSet fkeys = dbmd.getCrossReference(connection.getCatalog(),connection.getSchema(),other.getName(),connection.getCatalog(),connection.getSchema(),table.getName());
	      Metadata.DbIndex fkey = null;
	      while (fkeys.next()) {
	        String name = fkeys.getString("FK_NAME");
	        if (fkey == null || fkey.getName().equals(name)) {
	          fkey = new DbIndex(name,false);
	          table.getForeignKeys().put(name,fkey);
	          fkey.setReferences(other);
	        }
	        fkey.getFields().add(table.getFields().get(fkeys.getString("FKCOLUMN_NAME")));
	      }
    	}
    }
    return metadata;
  }
  
  
  private static class Separator {
  	final String token;
    boolean first = true;
    
    Separator() {
    	token = ",";
    }
    
    Separator(String token) {
    	this.token = token;
    }
    
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
  
  private String getType(DbField attribute) throws SQLException {
    JDBCType type = getJDBCType(attribute.getType());
    return typeName(type);
  }
  
  private void appendColumn(StringBuilder sql, String prefix, DbField attribute, boolean nullable) throws SQLException {
    if (!prefix.isEmpty()) {
      sql.append(prefix);
      sql.append('_');
    }
    appendColumn(sql,attribute,nullable);
  }
  
  private void appendColumn(StringBuilder sql, DbField attribute, boolean nullable) throws SQLException {
    sql.append(attribute.getName());
    sql.append(" ");
    sql.append(getType(attribute));
    if (!nullable) {
      sql.append(" NOT");
    }
    sql.append(" NULL");
  }
  
  protected void upgradeColumns(Connection connection,DbTable dbTable, DbClass entity) throws SQLException {
    TreeSet<String> fieldsToRemove = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    fieldsToRemove.addAll(dbTable.getFields().keySet());
    for (DbField attribute: entity.getFields()) {
      if (!fieldsToRemove.remove(attribute.getName())) {
        addFieldToTable(connection, entity, attribute);
      }
    }
    for (DbRole role: entity.getForeignKeys()) {
      for (DbField attribute: role.getForeignKey().getFields()) {
        if (!fieldsToRemove.remove(attribute.getName())) {
          addFieldToTable(connection,entity,attribute);
        }
      }
    }
    for (String name: fieldsToRemove) {
      removeFieldFromTable(connection,entity,name);
    }
  }
  
  protected void upgradeIndices(Connection connection,DbTable dbTable, DbClass entity) throws SQLException {
    TreeSet<String> indicesToRemove = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    indicesToRemove.addAll(dbTable.getIndices().keySet());
    indicesToRemove.addAll(dbTable.getForeignKeys().keySet());
    if (entity.getSuperClass() != null) {
      String name = FOREIGN_KEY_PREFIX + entity.getName() + "_parent";
      if (!indicesToRemove.remove(name)) {
        createParentKey(connection,entity);
      }
    }
    for (DbSearchMethod index: entity.getIndices().values()) {
      if (!indicesToRemove.remove(index.getName())) {
        createIndex(connection, index);
      }
    }
    for (DbRole role: entity.getForeignKeys()) {
      DbSearchMethod index = role.getForeignKey();
      if (!indicesToRemove.remove(index.getName())) {
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
  
  protected void addFieldToTable(Connection connection, DbClass entity, DbField attribute) throws SQLException {
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
    for (DbField attribute: entity.getFields()) {
      appendColumn(sql,attribute,attribute.isNullable());
      if (attribute.getId() != null && attribute.isGenerated() && entity.getSuperClass() == null) {
        sql.append(" GENERATED ALWAYS AS IDENTITY");
      }
      sql.append(",");
    }
    for (DbRole role: entity.getForeignKeys()) {
      for (DbField field: role.getForeignKey().getFields()) {
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
      Separator sqlComma = new Separator();
      Separator pkComma = new Separator();
      Separator ffkComma = new Separator();
      StringBuilder pk = new StringBuilder();
      StringBuilder ffk = new StringBuilder();
      for (DbField pkf: fk.getPrimaryKey().getFields()) {
        sqlComma.next(sql);
        pkComma.next(pk);
        ffkComma.next(ffk);
        appendColumn(sql,first.getName(),pkf,false);
        pk.append(first.getName() + '_' + pkf.getName());
        ffk.append(first.getName() + '_' + pkf.getName());
      }
      DbClass sk = Database.getDbClass(second.getType());
      Separator sfkComma = new Separator();
      StringBuilder sfk = new StringBuilder();
      for (DbField pkf: sk.getPrimaryKey().getFields()) {
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
      Separator sqlComma = new Separator();
      Separator pComma = new Separator();
      for (DbField field: entity.getFields()) {
        if (!field.isGenerated() || entity.getSuperClass() != null) {
          sqlComma.next(sql);
          pComma.next(params);
          sql.append(field.getName());
          params.append("?");
        }
      }
      for (DbRole role: entity.getForeignKeys()) {
        for (DbField field: role.getForeignKey().getFields()) {
          sqlComma.next(sql);
          pComma.next(params);
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
  
  protected Statements getUpdateStatement(Connection connection, DbClass entity) throws SQLException {
    Statements stmts = statements.computeIfAbsent(entity,e -> new Statements());
    if (stmts.getUpdate() == null) {
      StringBuilder sql = new StringBuilder("UPDATE ");
      sql.append(entity.getName());
      sql.append(" SET ");
      Separator sqlComma = new Separator();
      for (DbField field: entity.getFields()) {
      	if (field.isVersion()) {
          sqlComma.next(sql);
          sql.append(field.getName());
          sql.append('=');
          sql.append(field.getName());
          sql.append("+1");
      	} else if (!entity.getPrimaryKey().getFields().contains(field)) {
          sqlComma.next(sql);
          sql.append(field.getName());
          sql.append("=?");
        }
      }
      for (DbRole role: entity.getForeignKeys()) {
        for (DbField field: role.getForeignKey().getFields()) {
          sqlComma.next(sql);
          sql.append(field.getName());
          sql.append("=?");
        }
      }
      sql.append(" WHERE ");
      Separator and = new Separator(" AND ");
      for (DbField field: entity.getPrimaryKey().getFields()) {
      	and.next(sql);
      	sql.append(field.getName());
      	sql.append("=?");
      }
      if (entity.getVersion() != null) {
      	and.next(sql);
      	sql.append(entity.getVersion().getName());
      	sql.append("=?");
      }
      stmts.setUpdate(connection.prepareStatement(sql.toString(),Statement.RETURN_GENERATED_KEYS));
      stmts.setUpdateSql(sql.toString());
    }
    return stmts;
  }
  
  private void nextValue(StringBuilder sql, Object value) {
    if (sql != null) {
    	String literal = getLiteral(value);
      int pos = sql.indexOf("?");
      sql.replace(pos,pos+1,literal);
    }
  }
  
  public String getLiteral(Object value) {
  	if (value == null) {
  		return "null";
  	}
  	if (value.getClass() == String.class || value.getClass().getName().startsWith("java.time")) {
    	return '\'' + value.toString() + '\'';
  	}
		return value.toString();
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
    for (DbField field: entity.getFields()) {
      if (!field.isGenerated() || entity.getSuperClass() != null) {
        Object value = field.get(object);
        if (value != null) {
          stmt.setObject(index++,value);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
        }
        nextValue(sql,value);
      }
    }
    for (DbRole role: entity.getForeignKeys()) {
      Object child = role.get(object);
      DbClass cls = Database.getDbClass(role.getType());
      for (DbField field: cls.getPrimaryKey().getFields()) {
      	Object value = child == null ? null : field.get(child);
        if (value != null) {
          stmt.setObject(index++,value);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
        }
        nextValue(sql,value);
      }
    }
    if (logger != null) {
      logger.accept(sql.toString());
    }
    if (stmt.executeUpdate() != 0 && entity.getSuperClass() == null) {
      ResultSet keys = stmt.getGeneratedKeys();
      if (keys.next()) {
        index = 1;
        for (DbField field: entity.getPrimaryKey().getFields()) {
          if (field.isGenerated()) {
            field.set(object,keys.getObject(index++));
          }
        }
      }
    }
  }

  public void update(Connection connection, DbClass entity, Object object) throws SQLException {    if (entity.getSuperClass() != null) {
      update(connection,entity.getSuperClass(),object);
    }
    Statements stmts = getUpdateStatement(connection,entity);
    PreparedStatement stmt = stmts.getUpdate();
    StringBuilder sql = null;
    if (logger != null) {
      sql = new StringBuilder(stmts.getUpdateSql());
    }
    int index = 1;
    for (DbField field: entity.getFields()) {
      if (!entity.getPrimaryKey().getFields().contains(field) && !field.isVersion()) {
        Object value = field.get(object);
        if (value != null) {
          stmt.setObject(index++,value);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
        }
        nextValue(sql,value);
      }
    }
    for (DbRole role: entity.getForeignKeys()) {
      Object child = role.get(object);
      DbClass cls = Database.getDbClass(role.getType());
      for (DbField field: cls.getPrimaryKey().getFields()) {
      	Object value = child == null ? null : field.get(child);
        if (value != null) {
          stmt.setObject(index++,value);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
        }
        nextValue(sql,value);
      }
    }
    for (DbField field: entity.getPrimaryKey().getFields()) {
    	Object value = field.get(object);
    	stmt.setObject(index++, value);
    	nextValue(sql,value);
    }
    if (entity.getVersion() != null) {
    	Object value = entity.getVersion().get(object);
    	stmt.setObject(index++, value);
    	nextValue(sql,value);
    }
    if (logger != null) {
      logger.accept(sql.toString());
    }
    if (stmt.executeUpdate() != 1) {
    	throw new StaleObjectException(object + " is out of date.");
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
    Separator comma = new Separator();
    for (DbField field: path.getFields()) {
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
