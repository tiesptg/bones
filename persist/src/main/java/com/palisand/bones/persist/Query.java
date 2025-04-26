package com.palisand.bones.persist;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.palisand.bones.persist.CommandScheme.Separator;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;
import com.palisand.bones.persist.Database.DbClass.DbRole;
import lombok.Getter;
import lombok.Setter;

public class Query<X> {
  private final List<Object> selectObjects = new ArrayList<>();
  private StringBuilder select = new StringBuilder("SELECT ");
  private final Separator selectComma = new Separator();
  private StringBuilder from = new StringBuilder(" FROM ");
  private final CommandScheme commands;
  private ResultSet resultSet = null;
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

  private void selectColumns(DbClass cls, String alias) throws SQLException {
    if (cls.hasSubTypeField()) {
      selectComma.next(select);
      select.append(alias);
      select.append('.');
      select.append(CommandScheme.SUBTYPE_FIELD);
    }
    for (DbField field : cls.getFields()) {
      selectComma.next(select);
      select.append(alias);
      select.append('.');
      select.append(field.getName());
    }
    for (DbRole role : cls.getForeignKeys()) {
      for (DbField field : role.getForeignKey().getFields()) {
        selectComma.next(select);
        select.append(alias);
        select.append('.');
        select.append(field.getName());
      }
    }
  }

  public Query<X> selectFrom(Class<?> cls) throws SQLException {
    DbClass dbc = Database.getDbClass(cls);
    selectObjects.add(dbc);
    selectColumns(dbc, dbc.getName());
    from.append(dbc.getName());
    return this;
  }

  public Query<X> selectFrom(Class<?> cls, String alias) throws SQLException {
    selectObjects.add(cls);
    DbClass dbc = Database.getDbClass(cls);
    selectColumns(dbc, alias);
    from.append(dbc.getName());
    from.append(' ');
    from.append(alias);
    return this;
  }

  public Query<X> where(String clause) {
    add(" WHERE ", clause);
    return this;
  }

  private void add(String prefix, String rest) {
    from.append(prefix);
    from.append(rest);
  }

  public Query<X> and(String clause) {
    add(" AND ", clause);
    return this;
  }

  public Query<X> or(String clause) {
    add(" OR ", clause);
    return this;
  }

  public Query<X> execute() throws SQLException {
    StringBuilder sql = new StringBuilder(select).append(from);
    sql.append(" LIMIT ").append(rowsPerPage);
    sql.append(" OFFSET ").append((page - 1) * rowsPerPage);
    commands.log(sql.toString());
    resultSet = connection.createStatement().executeQuery(sql.toString());
    rowInPage = 0;
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
          Object result = cls.newInstance();
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
