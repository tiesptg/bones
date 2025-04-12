package com.palisand.bones.persist;

import java.sql.JDBCType;

@SuppressWarnings("serial")
public class PostgresqlCommands extends CommandScheme {

  @Override
  protected String typeName(JDBCType type) {
    if (type == JDBCType.DOUBLE) {
      return "DOUBLE PRECISION";
    }
    return super.typeName(type);
  }
  

}
