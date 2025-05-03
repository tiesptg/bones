package com.palisand.bones.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.palisand.bones.persist.CommandScheme.Separator;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;
import com.palisand.bones.persist.Database.DbClass.DbRole;
import com.palisand.bones.persist.Database.DbClass.DbSearchMethod;
import com.palisand.bones.persist.Database.StmtSetter;
import lombok.Getter;
import lombok.Setter;

public class Query<X> {

  public static class Operator {
    public static final String EQ = "=";
    public static final String NOT_EQ = "<>";
    public static final String LIKE = " LIKE ";
  }


  private final List<Object> selectObjects = new ArrayList<>();
  private final Map<String, DbClass> fromClasses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private StringBuilder select = new StringBuilder("SELECT ");
  private final Separator selectComma = new Separator();
  private StringBuilder from = new StringBuilder(" FROM ");
  private StringBuilder where = new StringBuilder();
  private final CommandScheme commands;
  private PreparedStatement stmt = null;
  private ResultSet resultSet = null;
  private final List<StmtSetter> setters = new ArrayList<>();
  private final List<Object> values = new ArrayList<>();
  private final Class<?> queryType;
  private final Connection connection;
  private int rowInPage = 0;
  @Getter
  private boolean lastPage = false;
  @Getter
  @Setter
  private int page = 1;
  @Getter
  @Setter
  private int rowsPerPage = 20;

  Query(Connection connection, CommandScheme commands, Class<?> queryType) throws SQLException {
    this.commands = commands;
    this.queryType = queryType;
    this.connection = connection;
  }

  private DbClass selectColumns(Class<?> cls) throws SQLException {
    DbClass dbc = Database.getDbClass(cls);
    return selectColumns(cls, dbc, dbc.getName());
  }

  private DbClass selectColumns(Class<?> cls, String alias) throws SQLException {
    DbClass dbc = Database.getDbClass(cls);
    return selectColumns(cls, dbc, alias);
  }


  private DbClass selectColumns(Class<?> cls, DbClass dbc, String alias) throws SQLException {
    selectObjects.add(dbc);
    fromClasses.put(alias, dbc);
    if (dbc.hasSubTypeField()) {
      selectComma.next(select);
      select.append(alias);
      select.append('.');
      select.append(CommandScheme.SUBTYPE_FIELD);
    }
    for (DbField field : dbc.getFields()) {
      selectComma.next(select);
      select.append(alias);
      select.append('.');
      select.append(field.getName());
    }
    for (DbRole role : dbc.getForeignKeys()) {
      for (DbField field : role.getForeignKey().getFields()) {
        selectComma.next(select);
        select.append(alias);
        select.append('.');
        select.append(field.getName());
      }
    }
    return dbc;
  }

  public Query<X> selectFrom(Class<?> cls) throws SQLException {
    DbClass dbc = selectColumns(cls);
    from.append(dbc.getName());
    return this;
  }

  public Query<X> selectFrom(Class<?> cls, String alias) throws SQLException {
    DbClass dbc = selectColumns(cls, alias);
    from.append(dbc.getName());
    from.append(' ');
    from.append(alias);
    return this;
  }

  private Object addParameter(String cls, String member) throws SQLException {
    DbClass dbc = fromClasses.get(cls);
    DbField field = dbc.getField(member);
    if (field != null) {
      setters.add(Database.STMT_SETTERS.get(field.getType()));
      return field;
    }
    DbRole role = dbc.getForeignKey(member);
    if (role == null) {
      role = dbc.getLink(member);
    }
    if (role != null) {
      for (DbField f : role.getForeignKey().getFields()) {
        setters.add(Database.STMT_SETTERS.get(f.getType()));
      }
    }
    return role;
  }

  public Query<X> where(String path, String operator, Object value) throws SQLException {
    String[] parts = path.split("\\.");
    Object member = addParameter(parts[0], parts[1]);
    if (member instanceof DbRole role) {
      if (!role.isForeignKey()) {
        role = role.getOpposite();
        // TODO: add join
      }
      Separator sep = new Separator(" AND ", " WHERE ");
      DbSearchMethod foreignKey = role.getForeignKey();
      DbClass type = Database.getDbClass(role.getType());
      for (int i = 0; i < foreignKey.getFields().size(); ++i) {
        DbField field = foreignKey.getFields().get(i);
        DbField pkField = type.getPrimaryKey().getFields().get(i);
        sep.next(where);
        where.append(parts[0]);
        where.append('.');
        where.append(field.getName());
        where.append(operator);
        where.append('?');

        Object key = pkField.get(value);
        values.add(key);
      }
    } else if (member instanceof DbField field) {
      add(" WHERE ", path + operator + '?');
      values.add(value);
    }
    return this;
  }

  private void add(String prefix, String rest) {
    where.append(prefix);
    where.append(rest);
  }

  public Query<X> and(String path, String operator, Object value) {
    add(" AND ", path + operator + '?');
    return this;
  }

  public Query<X> or(String path, String operator, Object value) {
    add(" OR ", path + operator + '?');
    return this;
  }

  private StringBuilder getSql() {
    StringBuilder sql = new StringBuilder(select).append(from).append(where);
    sql.append(" LIMIT ").append("?");
    sql.append(" OFFSET ").append("?");
    return sql;
  }

  private int getOffset() {
    return (page - 1) * rowsPerPage;
  }

  public Query<X> execute() throws SQLException {
    StringBuilder sql = getSql();
    String sqlstr = sql.toString();
    if (stmt == null) {
      stmt = commands.getQueryCache().get(sqlstr);
      if (stmt == null) {
        stmt = connection.prepareStatement(sqlstr);
        commands.getQueryCache().put(sqlstr, stmt);
      }
    }
    int index = 1;
    while (index <= values.size()) {
      StmtSetter setter = setters.get(index - 1);
      Object value = values.get(index - 1);
      setter.set(stmt, index++, value);
      CommandScheme.nextValue(sql, value);
    }
    stmt.setInt(index++, rowsPerPage);
    CommandScheme.nextValue(sql, rowsPerPage);
    int offset = getOffset();
    stmt.setInt(index++, offset);
    CommandScheme.nextValue(sql, offset);
    commands.log(sql.toString());
    resultSet = stmt.executeQuery();
    rowInPage = 0;
    setters.clear();
    values.clear();
    return this;
  }

  @SuppressWarnings("unchecked")
  private X mapRow(List<Object> row) {
    if (row.size() == 1 && queryType.isInstance(row.get(0))) {
      return (X) row.get(0);
    }
    return null;
  }

  public boolean nextPage() throws SQLException {
    if (!isLastPage()) {
      ++page;
      execute();
    }
    return false;
  }

  public X next() throws SQLException {
    if (resultSet.next()) {
      List<Object> row = new ArrayList<>();
      int index = 1;
      for (Object obj : selectObjects) {
        if (obj instanceof DbClass cls) {
          Object result;
          if (cls.hasSubTypeField()) {
            String label = resultSet.getString(index++);
            if (!cls.getLabel().equals(label)) {
              DbClass subClass = cls.getSubClasses().get(label);
              result = subClass.newInstance();
            } else {
              result = cls.newInstance();
            }
          } else {
            result = cls.newInstance();
          }
          index = commands.setPrimaryKey(resultSet, cls, result, index);
          result = commands.cache(cls, result);
          index = commands.setValues(resultSet, cls, result, index);
          row.add(result);
        }
      }
      ++rowInPage;
      return mapRow(row);
    }
    lastPage = rowInPage < rowsPerPage;
    return null;
  }



}
