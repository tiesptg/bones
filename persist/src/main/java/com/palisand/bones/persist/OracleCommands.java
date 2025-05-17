package com.palisand.bones.persist;

import java.sql.JDBCType;
import java.sql.SQLException;
import com.palisand.bones.persist.Database.DbClass.DbField;

public class OracleCommands extends CommandScheme {

  @Override
  protected String typeName(JDBCType type) {
    if (type == JDBCType.VARCHAR) {
      return "VARCHAR2";
    } else if (type == JDBCType.DOUBLE) {
      return "BINARY_DOUBLE";
    } else if (type == JDBCType.BIGINT) {
      return "INTEGER";
    }
    return super.typeName(type);
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



}
