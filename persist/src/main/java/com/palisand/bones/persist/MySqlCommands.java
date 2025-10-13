package com.palisand.bones.persist;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.UUID;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbField;

/**
 * The command scheme for the MySQL database
 */
public class MySqlCommands extends CommandScheme {

  @Override
  String typeName(JDBCType type, Class<?> cls, int size, int scale) {
    if (type == JDBCType.TIMESTAMP_WITH_TIMEZONE) {
      return "TIMESTAMP";
    } else if (type == JDBCType.DECIMAL) {
      if (size == 0)
        size = 10;
      if (scale == 0)
        scale = 2;
    } else if (type == JDBCType.CLOB) {
      return "TEXT";
    } else if (cls == UUID.class) {
      size = 36;
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
    return " AUTO_INCREMENT";
  }

  void dropIndex(Connection connection, DbClass entity, String indexName) throws SQLException {
    StringBuilder sql = new StringBuilder("DROP INDEX ");
    sql.append(indexName);
    sql.append(" ON ").append(entity.getName());
    execute(connection, sql.toString());
  }

}
