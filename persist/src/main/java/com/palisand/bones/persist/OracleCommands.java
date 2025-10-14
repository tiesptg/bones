package com.palisand.bones.persist;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbField;

/**
 * The CommandScheme for Oracle
 */
public class OracleCommands extends CommandScheme {
  @Override
  String typeName(JDBCType type, Class<?> cls, int size, int scale) {
    if (type == JDBCType.VARCHAR) {
      return "VARCHAR2(" + size + ')';
    } else if (type == JDBCType.DOUBLE) {
      return "BINARY_DOUBLE";
    } else if (type == JDBCType.BIGINT) {
      return "INTEGER";
    } else if (type == JDBCType.VARBINARY) {
      return "LONG RAW";
    } else if (type == JDBCType.TIME || type == JDBCType.DATE) {
      return "TIMESTAMP";
    }
    return super.typeName(type, cls, size, scale);
  }

  @Override
  int getSize(DbField attribute) throws SQLException {
    int size = attribute.getSize();
    if (getJDBCType(attribute) == JDBCType.VARCHAR && size == 0) {
      return 4000;
    }
    return size;
  }

  @Override
  JDBCType getJDBCType(int type) {
    if (type == 101) {
      return JDBCType.DOUBLE;
    } else if (type == -101) {
      return JDBCType.TIMESTAMP;
    }
    return super.getJDBCType(type);
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

  @Override
  PreparedStatement prepareInsertStatement(Connection connection, DbClass entity, String sql)
      throws SQLException {
    return connection.prepareStatement(sql, entity.getPkFieldNumbers());
  }

}
