package com.palisand.bones.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import com.palisand.bones.persist.CommandScheme.Separator;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;
import com.palisand.bones.persist.Database.DbClass.DbRole;
import com.palisand.bones.persist.Database.DbClass.DbSearchMethod;
import com.palisand.bones.persist.Database.StmtSetter;
import lombok.Getter;
import lombok.Setter;

public class Query<X> {

  public static final String EQ = "=";
  public static final String NOT_EQ = "<>";
  public static final String LIKE = " LIKE ";
  public static final String LT = "<";
  public static final String GT = ">";

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

  public Query<X> select(String... columns) throws SQLException {
    for (String column : columns) {
      addSelectObject(selectObjects.size());
      selectComma.next(select);
      select.append(column);
    }
    return this;
  }

  private void addSelectObject(int position) {
    if (queryType.isRecord()) {
      selectObjects.add(queryType.getRecordComponents()[position].getType());
    } else if (position == 0) {
      selectObjects.add(queryType);
    }
  }

  public Query<X> from(Class<?> cls) throws SQLException {
    return from(cls, null);
  }

  public Query<X> from(Class<?> cls, String alias) throws SQLException {
    if (from.length() > 6) { // ' FROM '
      from.append(',');
    }
    DbClass entity = Database.getDbClass(cls);
    from.append(entity.getName());
    if (alias != null) {
      from.append(' ').append(alias);
    }
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

  private void constructJoin(String fromName, DbClass toClass, String alias, String joinType,
      List<DbField> fromFields, List<DbField> toFields) {
    from.append(joinType).append(toClass.getName());
    if (alias != null) {
      from.append(' ').append(alias);
    }
    from.append(" ON ");
    if (alias == null) {
      alias = toClass.getName();
    }
    fromClasses.put(alias, toClass);
    Separator and = new Separator(" AND ");
    for (int j = 0; j < fromFields.size(); ++j) {
      DbField field = fromFields.get(j);
      DbField pkField = toFields.get(j);
      and.next(from);
      from.append(fromName).append('.').append(field.getName()).append('=').append(alias)
          .append('.').append(pkField.getName());
    }

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
        constructJoin(className, type, alias, joinType, foreignKey.getFields(),
            type.getPrimaryKey().getFields());
      } else {
        DbRole opposite = role.getOpposite();
        if (opposite != null) {
          if (opposite.isForeignKey()) {
            DbSearchMethod foreignKey = opposite.getForeignKey();
            DbClass type = foreignKey.getEntity();
            constructJoin(className, type, alias, joinType, type.getPrimaryKey().getFields(),
                foreignKey.getFields());
          } else {
            // TODO: many to many relation
          }
        }
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

  public Query<X> brackets(Consumer<Query<X>> queryClause) {
    where.append('(');
    queryClause.accept(this);
    where.append(')');
    return this;
  }

  public Query<X> orderBy(String orderBy) {
    where.append(" ORDER BY ").append(orderBy);
    return this;
  }

  public Query<X> groupBy(String... columns) {
    Separator comma = new Separator();
    where.append(" GROUP BY ");
    for (String column : columns) {
      comma.next(where);
      where.append(column);
    }
    return this;
  }

  private StringBuilder getSql() {
    StringBuilder sql = new StringBuilder(select).append(from).append(where);
    commands.addSelectPage(sql);
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
    int offset = getOffset();
    index = commands.setSelectPageValues(stmt, sql, rowsPerPage, offset, index);
    commands.log(sql.toString());
    resultSet = stmt.executeQuery();
    rowInPage = 0;
    setters.clear();
    values.clear();

    return this;
  }

  @SuppressWarnings("unchecked")
  private X mapRow(List<Object> row) throws SQLException {
    if (row.size() == 1 && queryType.isInstance(row.get(0))) {
      return (X) row.get(0);
    } else if (queryType.isRecord()) {
      try {
        return (X) queryType.getDeclaredConstructors()[0].newInstance(row.toArray());
      } catch (Exception ex) {
        if (ex.getCause() != null) {
          throw new SQLException(ex.getCause());
        }
        throw new SQLException(ex);
      }
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
        } else if (obj instanceof Class<?> cls) {
          row.add(Database.RS_GETTERS.get(cls).get(resultSet, index++));
        }
      }
      ++rowInPage;
      return mapRow(row);
    }
    lastPage = rowInPage < rowsPerPage;
    return null;
  }

  public List<X> toList() throws SQLException {
    List<X> list = new ArrayList<>();
    setPage(1);
    if (resultSet == null) {
      execute();
    }
    while (!isLastPage()) {
      for (X x = next(); x != null; x = next()) {
        list.add(x);
      }
      nextPage();
    }
    return list;
  }

  public List<X> pageToList() throws SQLException {
    List<X> list = new ArrayList<>();
    if (resultSet == null) {
      execute();
    }
    for (X x = next(); x != null; x = next()) {
      list.add(x);
    }
    return list;
  }

}
