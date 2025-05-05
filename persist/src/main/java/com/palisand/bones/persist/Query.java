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
  private int aliasPostfix = 1;
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

  private DbClass selectColumns(Class<?> cls, String alias) throws SQLException {
    DbClass dbc = Database.getDbClass(cls);
    return selectColumns(cls, dbc, alias);
  }

  private String getAlias(DbClass type, DbClass reference, String alias) {
    if (type != reference && type.getLabel() != null) {
      return type.getLabel() + (aliasPostfix++);
    }
    if (alias == null) {
      return reference.getName();
    }
    return alias;
  }


  private DbClass selectColumns(Class<?> cls, DbClass dbc, String alias) throws SQLException {
    if (alias == null) {
      alias = dbc.getName();
    }
    selectObjects.add(dbc);
    fromClasses.put(alias, dbc);
    DbClass root = dbc.getRoot();
    String rootAlias = getAlias(root, dbc, alias);
    if (dbc.getRoot().hasSubTypeField()) {
      selectComma.next(select);
      select.append(rootAlias);
      select.append('.');
      select.append(CommandScheme.SUBTYPE_FIELD);
    }
    boolean first = true;
    List<DbClass> hierarchy = dbc.getTypeHierarchy();
    for (DbClass type : hierarchy) {
      String typeAlias = type == root ? rootAlias : getAlias(type, dbc, alias);
      fromClasses.put(typeAlias, type);
      for (DbField field : type.getFields()) {
        if (first || !type.getPrimaryKey().getFields().contains(field)) {
          selectComma.next(select);
          select.append(typeAlias);
          select.append('.');
          select.append(field.getName());
        }
      }
      for (DbRole role : type.getForeignKeys()) {
        for (DbField field : role.getForeignKey().getFields()) {
          selectComma.next(select);
          select.append(typeAlias);
          select.append('.');
          select.append(field.getName());
        }
      }
      first = false;
    }
    return dbc;
  }

  public Query<X> selectFrom(Class<?> cls) throws SQLException {
    return selectFrom(cls, null);
  }

  public Query<X> selectFrom(Class<?> cls, String alias) throws SQLException {
    DbClass dbc = selectColumns(cls, alias);
    commands.addHierarchyJoins(from, dbc.getTypeHierarchy(), dbc, fromClasses);
    return this;
  }

  public Query<X> join(String path) throws SQLException {
    return join(path, null);
  }

  public Query<X> join(String path, String alias) throws SQLException {
    String[] parts = path.split("\\.");
    addJoin(parts[0], parts[1], " JOIN ", alias);
    return this;
  }

  private String addJoin(String className, String memberName, String joinType, String alias)
      throws SQLException {
    DbClass fromClass = fromClasses.get(className);
    if (fromClass == null) {
      throw new SQLException("class name or alias " + className + " not found in query");
    }
    DbRole role = fromClass.getForeignKey(memberName);
    if (role == null) {
      role = fromClass.getLink(memberName);
    }
    if (role != null) {
      if (role.isForeignKey()) {
        DbSearchMethod foreignKey = role.getForeignKey();
        DbClass type = Database.getDbClass(role.getType());
        from.append(" JOIN ").append(type.getName());
        if (alias != null) {
          from.append(' ').append(alias);
        }
        from.append(" ON ");
        if (alias == null) {
          alias = type.getName();
        }
        fromClasses.put(alias, type);
        Separator and = new Separator(" AND ");
        for (int j = 0; j < foreignKey.getFields().size(); ++j) {
          DbField field = foreignKey.getFields().get(j);
          DbField pkField = type.getPrimaryKey().getFields().get(j);
          and.next(from);
          from.append(className).append('.').append(field.getName()).append('=').append(alias)
              .append('.').append(pkField.getName());
        }
      } else {
        // TODO
      }
    } else {
      throw new SQLException("Role " + memberName + " not found in class " + fromClass.getName());
    }
    return alias;
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
    String alias = parts[0];
    for (int i = 1; i < parts.length - 1; ++i) {
      alias = addJoin(alias, parts[i], " JOIN ", "x" + (aliasPostfix++));
    }
    return addCondition(alias, parts[parts.length - 1], " WHERE ", operator, value);
  }

  private Query<X> addCondition(String className, String memberName, String initialSeparator,
      String operator, Object value) throws SQLException {
    Object member = addParameter(className, memberName);
    if (member instanceof DbRole role) {
      if (!role.isForeignKey()) {
        role = role.getOpposite();
        // TODO: add join
      }
      Separator sep = new Separator(" AND ", initialSeparator);
      DbSearchMethod foreignKey = role.getForeignKey();
      DbClass type = Database.getDbClass(role.getType());
      for (int i = 0; i < foreignKey.getFields().size(); ++i) {
        DbField field = foreignKey.getFields().get(i);
        DbField pkField = type.getPrimaryKey().getFields().get(i);
        sep.next(where);
        where.append(className);
        where.append('.');
        where.append(field.getName());
        where.append(operator);
        where.append('?');

        Object key = pkField.get(value);
        values.add(key);
      }
    } else if (member instanceof DbField) {
      where.append(initialSeparator).append(className).append('.').append(memberName)
          .append(operator).append('?');
      values.add(value);
    }
    return this;
  }

  public Query<X> and(String path, String operator, Object value) throws SQLException {
    String[] parts = path.split("\\.");
    return addCondition(parts[0], parts[1], " AND ", operator, value);
  }

  public Query<X> or(String path, String operator, Object value) throws SQLException {
    String[] parts = path.split("\\.");
    return addCondition(parts[0], parts[1], " OR ", operator, value);
  }

  public Query<X> orderBy(String orderBy) {
    where.append(" ORDER BY ").append(orderBy);
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
          String label = null;
          DbClass realCls = cls;
          if (cls.getRoot().hasSubTypeField()) {
            label = resultSet.getString(index++);
            realCls = cls.getLabel().equals(label) ? cls : cls.getSubClasses().get(label);
          }
          Object result = realCls.newInstance();
          index = commands.setPrimaryKey(resultSet, cls, result, index);
          result = commands.cache(cls, result);
          index = commands.setHierarchyValues(resultSet, cls, realCls, result, index);
          row.add(result);
        } else {
          // TODO simple values
        }
      }
      ++rowInPage;
      return mapRow(row);
    }
    lastPage = rowInPage < rowsPerPage;
    return null;
  }



}
