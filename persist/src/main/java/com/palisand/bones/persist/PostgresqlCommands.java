package com.palisand.bones.persist;

import java.sql.JDBCType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.palisand.bones.persist.Database.RsGetter;
import com.palisand.bones.persist.Database.StmtSetter;

public class PostgresqlCommands extends CommandScheme {
  static final Map<Class<?>, RsGetter> RS_GETTERS = new HashMap<>();
  static final Map<Class<?>, StmtSetter> STMT_SETTERS = new HashMap<>();

  static {
    RS_GETTERS.putAll(CommandScheme.RS_GETTERS);
    STMT_SETTERS.putAll(CommandScheme.STMT_SETTERS);
    RS_GETTERS.put(UUID.class, (rs, pos) -> rs.getObject(pos, UUID.class));
    STMT_SETTERS.put(UUID.class, (rs, pos, value) -> rs.setObject(pos, (UUID) value));
  }

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

  @Override
  RsGetter getRsGetter(Class<?> cls) {
    return RS_GETTERS.get(cls);
  }

  @Override
  StmtSetter getStmtSetter(Class<?> cls) {
    return STMT_SETTERS.get(cls);
  }

}
