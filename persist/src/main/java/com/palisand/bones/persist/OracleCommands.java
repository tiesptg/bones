package com.palisand.bones.persist;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;

public class OracleCommands extends CommandScheme {

  @Override
  protected String typeName(JDBCType type, Class<?> cls, int size, int scale) {
    if (type == JDBCType.VARCHAR) {
      return "VARCHAR2(" + size + ')';
    } else if (type == JDBCType.DOUBLE) {
      return "BINARY_DOUBLE";
    } else if (type == JDBCType.BIGINT) {
      return "INTEGER";
    }
    return super.typeName(type, cls, size, scale);
  }

  @Override
  protected int getSize(DbField attribute) throws SQLException {
    int size = attribute.getSize();
    if (getJDBCType(attribute) == JDBCType.VARCHAR && size == 0) {
      return 4000;
    }
    return size;
  }

  @Override
  protected JDBCType getJDBCType(int type) {
    if (type == 101) {
      return JDBCType.DOUBLE;
    }
    return super.getJDBCType(type);
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

  @Override
  protected PreparedStatement prepareInsertStatement(Connection connection, DbClass entity,
      String sql) throws SQLException {
    return connection.prepareStatement(sql, entity.getPkFieldNumbers());
  }

}
