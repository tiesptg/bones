package com.palisand.bones.persist;

import java.sql.JDBCType;
import java.util.UUID;

public class PostgresqlCommands extends CommandScheme {

  @Override
  protected String typeName(JDBCType type, Class<?> cls, int size, int scale) {
    if (type == JDBCType.DOUBLE) {
      return "DOUBLE PRECISION";
    } else if (type == JDBCType.VARBINARY || type == JDBCType.BLOB) {
      return "BYTEA";
    } else if (type == JDBCType.CLOB) {
      return "TEXT";
    } else if (cls == UUID.class) {
      return "UUID";
    }
    return super.typeName(type, cls, size, scale);
  }


}
