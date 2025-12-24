package com.palisand.bones.persist;

import java.sql.JDBCType;
import java.util.UUID;

/**
 * The CommandScheme for the PostgreSQL database
 */
public class PostgresqlCommands extends CommandScheme {

  static {
    RS_GETTERS.putAll(CommandScheme.RS_GETTERS);
    STMT_SETTERS.putAll(CommandScheme.STMT_SETTERS);
    RS_GETTERS.put(UUID.class, (rs, pos) -> rs.getObject(pos, UUID.class));
    STMT_SETTERS.put(UUID.class, (rs, pos, value) -> rs.setObject(pos, value));
  }

  @Override
  String typeName(JDBCType type, Class<?> cls, int size, int scale) {
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
