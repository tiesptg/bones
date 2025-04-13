package com.palisand.bones.persist;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.palisand.bones.persist.Database.Entity;
import com.palisand.bones.persist.Database.Entity.Attribute;
import com.palisand.bones.persist.Database.Entity.Role;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
public class CommandScheme extends HashMap<String,Class<?>> {
  private Map<Class<?>,JDBCType> typeMap = new HashMap<>();
  protected static final String FOREIGN_KEY_PREFIX = "fk_";
  protected Map<Entity,Statements> statements = new HashMap<>();
  
  @Getter
  @Setter
  private class Statements {
    private PreparedStatement insert;
    private String insertSql;
    private PreparedStatement update;
    private String updateSql;
    private PreparedStatement delete;
    private String deleteSql;
    private PreparedStatement selectOne;
    private String selectOneSql;
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
  
  protected String typeName(JDBCType type) {
    return type.getName();
  }
  
  protected boolean execute(Connection connection, String sql) throws SQLException {
    System.out.println(sql);
    return connection.createStatement().execute(sql);
  }
  
  private JDBCType getJDBCType(Class<?> cls) throws SQLException {
    JDBCType type = typeMap.get(cls);
    if (type == null) {
      throw new SQLException("Type " + cls + " is not (yet) supported");
    }
    return type;
  }
  
  private String getType(Attribute attribute) throws SQLException {
    JDBCType type = getJDBCType(attribute.getType());
    return typeName(type);
  }
  
  private void appendField(StringBuilder sql, String prefix, Attribute attribute, boolean nullable) throws SQLException {
    sql.append(prefix);
    sql.append(attribute.getName());
    sql.append(" ");
    sql.append(getType(attribute));
    if (!nullable) {
      sql.append(" NOT");
    }
    sql.append(" NULL");
  }
  
  public void createTable(Connection connection, Entity entity) throws SQLException {
    StringBuilder sql = new StringBuilder("CREATE TABLE ");
    sql.append(entity.getName());
    sql.append("(");
    for (Attribute attribute: entity.getFields()) {
      appendField(sql,"",attribute,attribute.isNullable());
      if (attribute.getId() != null && attribute.getId().generated()) {
        sql.append(" GENERATED ALWAYS AS IDENTITY");
      }
      sql.append(",");
    }
    for (Role role: entity.getForeignKeys()) {
      Entity fk = Database.getEntity(role.getType());
      for (Attribute pkf: fk.getPrimaryKey().getFields()) {
        appendField(sql,role.getName(),pkf,true);
        sql.append(",");
      }
    }
    sql.append("PRIMARY KEY(");
    sql.append(entity.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append("))");
    execute(connection, sql.toString());
  }
  
  public void createLinkTable(Connection connection, Role first) throws SQLException {
    Role second = first.getOpposite();
    if (second != null) {
      StringBuilder sql = new StringBuilder("CREATE TABLE ");
      sql.append(first.getName());
      Entity entity = Database.getEntity(first.getType());
      sql.append(entity.getName());
      sql.append("(");
      Entity fk = Database.getEntity(first.getType());
      boolean comma = false;
      StringBuilder pk = new StringBuilder();
      StringBuilder ffk = new StringBuilder();
      for (Attribute pkf: fk.getPrimaryKey().getFields()) {
        if (comma) {
          sql.append(",");
          pk.append(",");
          ffk.append(",");
        } else {
          comma = true;
        }
        appendField(sql,first.getName(),pkf,false);
        pk.append(first.getName() + pkf.getName());
        ffk.append(first.getName() + pkf.getName());
      }
      Entity sk = Database.getEntity(second.getType());
      comma = false;
      StringBuilder sfk = new StringBuilder();
      for (Attribute pkf: sk.getPrimaryKey().getFields()) {
        sql.append(",");
        pk.append(",");
        if (comma) {
          sfk.append(",");
        } else {
          comma = true;
        }
        appendField(sql,second.getName(),pkf,false);
        pk.append(second.getName() + pkf.getName());
        sfk.append(second.getName() + pkf.getName());
      }
      sql.append(",PRIMARY KEY(");
      sql.append(pk.toString());
      sql.append("),FOREIGN KEY(");
      sql.append(ffk.toString());
      sql.append(") REFERENCES ");
      sql.append(fk.getName());
      sql.append("(");
      sql.append(fk.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
      sql.append("),FOREIGN KEY(");
      sql.append(sfk.toString());
      sql.append(") REFERENCES ");
      sql.append(sk.getName());
      sql.append("(");
      sql.append(sk.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
      sql.append("))");
      execute(connection,sql.toString());
    }
  }
  
  public void dropTable(Connection connection, Entity entity) throws SQLException {
    StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS ");
    sql.append(entity.getName());
    execute(connection,sql.toString());
  }

  public void dropContraint(Connection connection, Entity entity, Role role) throws SQLException {
    StringBuilder sql = new StringBuilder("ALTER TABLE IF EXISTS ");
    sql.append(entity.getName());
    sql.append(" DROP CONSTRAINT IF EXISTS ");
    sql.append(FOREIGN_KEY_PREFIX);
    sql.append(role.getName());
    execute(connection,sql.toString());
  }

  public void dropTable(Connection connection, Role first) throws SQLException {
    Role second = first.getOpposite();
    if (second != null) {
      StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS ");
      sql.append(first.getName());
      Entity entity = Database.getEntity(first.getType());
      sql.append(entity.getName());
      execute(connection,sql.toString());
    }
  }

  public void createForeignKey(Connection connection, Role role) throws SQLException {
    StringBuilder sql = new StringBuilder("ALTER TABLE IF EXISTS ");
    sql.append(role.getEntity().getName());
    sql.append(" ADD CONSTRAINT ");
    sql.append(FOREIGN_KEY_PREFIX);
    Entity fk = Database.getEntity(role.getType());
    sql.append(role.getName());
    sql.append(" FOREIGN KEY(");
    sql.append(fk.getPrimaryKey().getFields().stream().map(a -> role.getName() + a.getName()).collect(Collectors.joining(",")));
    sql.append(") REFERENCES ");
    sql.append(fk.getName());
    sql.append("(");
    sql.append(fk.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append(")");
    execute(connection,sql.toString());
    
  }
  
  protected PreparedStatement getInsertStatement(Connection connection, Entity entity) throws SQLException {
    Statements stmts = statements.computeIfAbsent(entity,e -> new Statements());
    if (stmts.getInsert() == null) {
      StringBuilder sql = new StringBuilder("INSERT INTO ");
      StringBuilder params = new StringBuilder();
      sql.append(entity.getName());
      sql.append("(");
      Comma sqlComma = new Comma();
      Comma pComma = new Comma();
      for (Attribute field: entity.getFields()) {
        if (!field.isGenerated()) {
          sqlComma.next(sql);
          pComma.next(params);
          sql.append(field.getName());
          params.append("?");
        }
      }
      for (Role role: entity.getForeignKeys()) {
        Entity other = Database.getEntity(role.getType());
        for (Attribute field: other.getPrimaryKey().getFields()) {
          sqlComma.next(sql);
          pComma.next(params);
          sql.append(role.getName());
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
    System.out.println(stmts.getInsertSql());
    return stmts.getInsert();
  }

  public void insert(Connection connection, Entity entity, Object object) throws SQLException {
    PreparedStatement stmt = getInsertStatement(connection,entity);
    int index = 1;
    for (Attribute field: entity.getFields()) {
      if (!field.isGenerated()) {
        Object value = field.get(object);
        if (value != null) {
          stmt.setObject(index++,value);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
        }
      }
    }
    for (Role role: entity.getForeignKeys()) {
      Object value = role.get(object);
      Entity other = Database.getEntity(role.getType());
      for (Attribute field: other.getPrimaryKey().getFields()) {
        if (value != null) {
          Object key =  field.get(value);
          stmt.setObject(index++,key);
        } else {
          stmt.setNull(index++,getJDBCType(field.getType()).ordinal());
        }
      }
    }
    if (stmt.execute()) {
      ResultSet keys = stmt.getResultSet();
      if (keys.next()) {
        index = 1;
        for (Attribute field: entity.getPrimaryKey().getFields()) {
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
 
}
