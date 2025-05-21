package com.palisand.bones.persist;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;

public class MySqlCommands extends CommandScheme {

  @Override
  protected int getSize(DbField attribute) throws SQLException {
    int size = attribute.getSize();
    if (getJDBCType(attribute) == JDBCType.VARCHAR && size == 0) {
      return 4000;
    }
    return size;
  }

  @Override
  protected String getGeneratedClause() {
    return " AUTO_INCREMENT";
  }

  protected void dropIndex(Connection connection, DbClass entity, String indexName)
      throws SQLException {
    StringBuilder sql = new StringBuilder("DROP INDEX ");
    sql.append(indexName);
    sql.append(" ON ").append(entity.getName());
    execute(connection, sql.toString());
  }

}
