package com.palisand.bones.persist;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;
import com.palisand.bones.persist.Database.RsGetter;
import com.palisand.bones.persist.Database.StmtSetter;

public class MsSqlServerCommands extends CommandScheme {
  static final Map<Class<?>, RsGetter> RS_GETTERS = new HashMap<>();
  static final Map<Class<?>, StmtSetter> STMT_SETTERS = new HashMap<>();

  static {
    RS_GETTERS.putAll(CommandScheme.RS_GETTERS);
    STMT_SETTERS.putAll(CommandScheme.STMT_SETTERS);
    RS_GETTERS.put(BigInteger.class, (rs, pos) -> {
      BigDecimal value = rs.getObject(pos, BigDecimal.class);
      if (value != null) {
        return value.toBigInteger();
      }
      return null;
    });
    STMT_SETTERS.put(BigInteger.class,
        (rs, pos, value) -> rs.setObject(pos, new BigDecimal((BigInteger) value)));
  }

  @Override
  protected JDBCType getJDBCType(int type) {
    if (type == -155) {
      return JDBCType.TIMESTAMP_WITH_TIMEZONE;
    }
    return super.getJDBCType(type);
  }

  @Override
  RsGetter getRsGetter(Class<?> cls) {
    return RS_GETTERS.get(cls);
  }

  @Override
  StmtSetter getStmtSetter(Class<?> cls) {
    return STMT_SETTERS.get(cls);
  }

  @Override
  protected String typeName(JDBCType type, Class<?> cls, int size, int scale) {
    if (type == JDBCType.DOUBLE) {
      return "REAL";
    } else if (type == JDBCType.BOOLEAN) {
      return "BIT";
    } else if (type == JDBCType.TIMESTAMP_WITH_TIMEZONE) {
      return "DATETIMEOFFSET";
    } else if (type == JDBCType.TIMESTAMP) {
      return "DATETIME2";
    } else if (type == JDBCType.CLOB) {
      return "TEXT";
    } else if (type == JDBCType.BLOB) {
      return "IMAGE";
    }
    return super.typeName(type, cls, size, scale);
  }

  @Override
  protected int getSize(DbField attribute) throws SQLException {
    int size = attribute.getSize();
    JDBCType type = getJDBCType(attribute);
    if ((type == JDBCType.VARCHAR || type == JDBCType.VARBINARY) && size == 0) {
      return 4000;
    }
    return size;
  }

  @Override
  protected String getGeneratedClause() {
    return " IDENTITY(1,1)";
  }

  @Override
  protected void dropIndex(Connection connection, DbClass entity, String indexName)
      throws SQLException {
    StringBuilder sql = new StringBuilder("DROP INDEX ");
    sql.append(indexName);
    sql.append(" ON ").append(entity.getName());
    execute(connection, sql.toString());
  }

  @Override
  public void addSelectPage(StringBuilder sql) {
    sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
  }

  @Override
  public int setSelectPageValues(PreparedStatement stmt, StringBuilder sql, int limit, int offset,
      int index) throws SQLException {
    stmt.setInt(index++, offset);
    CommandScheme.nextValue(sql, offset);
    stmt.setInt(index++, limit);
    CommandScheme.nextValue(sql, limit);
    return index;
  }

}
