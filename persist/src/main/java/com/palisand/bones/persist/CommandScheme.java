package com.palisand.bones.persist;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.palisand.bones.persist.Database.Entity;
import com.palisand.bones.persist.Database.Entity.Attribute;

@SuppressWarnings("serial")
public class CommandScheme extends HashMap<String,Class<?>> {
  private Map<Class<?>,JDBCType> typeMap = new HashMap<>();
  
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
  
  private String getType(Attribute attribute) throws SQLException {
    JDBCType type = typeMap.get(attribute.getType());
    if (type == null) {
      throw new SQLException("Type " + attribute.getType() + " is not (yet) supported");
    }
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
  
  public void createTable(Connection connection, Class<?> cls) throws SQLException {
    Entity entity = Database.getEntity(cls);
    StringBuilder sql = new StringBuilder("CREATE TABLE ");
    sql.append(entity.getName());
    sql.append("(");
    for (Attribute attribute: entity.getFields()) {
      if (attribute.getType().isPrimitive() && !attribute.getType().getName().startsWith("java")) {
        appendField(sql,"",attribute,attribute.isNullable());
        if (attribute.getId() != null && attribute.getId().generated()) {
          sql.append(" GENERATED ALWAYS AS IDENTITY");
        }
        sql.append(",");
      } else {
        Entity fk = Database.getEntity(attribute.getType());
        for (Attribute pkf: fk.getPrimaryKey().getFields()) {
          appendField(sql,attribute.getName(),pkf,attribute.isNullable());
          sql.append(",");
        }
      }
    }
    sql.append("PRIMARY KEY(");
    sql.append(entity.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
    sql.append("))");
    execute(connection, sql.toString());
  }
  
  public void dropTable(Connection connection, Class<?> cls) throws SQLException {
    Entity entity = Database.getEntity(cls);
    StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS ");
    sql.append(entity.getName());
    execute(connection,sql.toString());
  }
 
}
