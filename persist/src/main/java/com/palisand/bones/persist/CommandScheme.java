package com.palisand.bones.persist;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
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
  protected Map<String,Statements> statements = new HashMap<>();
  
  @Getter
  @Setter
  private class Statements {
    private PreparedStatement insert;
    private PreparedStatement update;
    private PreparedStatement delete;
    private PreparedStatement selectOne;
  }
  
  private abstract class ObjectStreamer implements SQLData {
    protected Entity type;

    @Override
    public String getSQLTypeName() throws SQLException {
      return type.getName();
    }

    @Override
    public void readSQL(SQLInput stream, String typeName) throws SQLException {
    }

    @Override
    public void writeSQL(SQLOutput stream) throws SQLException {
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
  
  public void register(Entity entity) {
    put(entity.getName(),new ObjectStreamer() {{type=entity;}}.getClass());
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
  
  public void createTable(Connection connection, Entity entity) throws SQLException {
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
      }
    }
    for (Role role: entity.getForeignKeys()) {
      if (!role.isMany()) {
        Entity fk = Database.getEntity(role.getType());
        for (Attribute pkf: fk.getPrimaryKey().getFields()) {
          appendField(sql,role.getName(),pkf,true);
          sql.append(",");
        }
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

  public void insert(Connection connection, Entity entity, Object object) {
    
  }

  public String getDatabaseName(Connection connection) throws SQLException {
    return connection.getCatalog();
  }
 
}
