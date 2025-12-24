package com.palisand.bones.persist;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbField;

/**
 * The CommandScheme for MS SQL Server
 */
public class MsSqlServerCommands extends CommandScheme {

  static {
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
  JDBCType getJDBCType(int type) {
    if (type == -155) {
      return JDBCType.TIMESTAMP_WITH_TIMEZONE;
    }
    return super.getJDBCType(type);
  }

  @Override
  String typeName(JDBCType type, Class<?> cls, int size, int scale) {
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
  int getSize(DbField attribute) throws SQLException {
    int size = attribute.getSize();
    JDBCType type = getJDBCType(attribute);
    if ((type == JDBCType.VARCHAR || type == JDBCType.VARBINARY) && size == 0) {
      return 4000;
    }
    return size;
  }

  @Override
  String getGeneratedClause() {
    return " IDENTITY(1,1)";
  }

  @Override
  void dropIndex(Connection connection, DbClass entity, String indexName) throws SQLException {
    StringBuilder sql = new StringBuilder("DROP INDEX ");
    sql.append(indexName);
    sql.append(" ON ").append(entity.getName());
    execute(connection, sql.toString());
  }

  @Override
  void addSelectPage(StringBuilder sql) {
    sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
  }

  @Override
  int setSelectPageValues(PreparedStatement stmt, StringBuilder sql, int limit, int offset,
      int index) throws SQLException {
    stmt.setInt(index++, offset);
    CommandScheme.nextValue(sql, offset);
    stmt.setInt(index++, limit);
    CommandScheme.nextValue(sql, limit);
    return index;
  }

}
