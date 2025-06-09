package com.palisand.bones.persist;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import com.palisand.bones.persist.CommandScheme.Separator;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;
import com.palisand.bones.persist.Database.DbClass.DbForeignKeyField;
import com.palisand.bones.persist.Database.DbClass.DbRole;
import com.palisand.bones.persist.Database.DbClass.DbSearchMethod;
import com.palisand.bones.persist.Database.StmtSetter;
import lombok.Getter;

public class Query<X> implements Closeable {

  public static final String EQ = "=";
  public static final String NOT_EQ = "<>";
  public static final String LIKE = " LIKE ";
  public static final String LT = "<";
  public static final String GT = ">";

  private record Join(DbClass fromClass, String alias, DbClass toClass, DbSearchMethod role,
      String joinType) {
  }

  private class OrderedSet<X> extends AbstractSet<X> {
    private final ArrayList<X> list = new ArrayList<>();

    @Override
    public Iterator<X> iterator() {
      return list.iterator();
    }

    @Override
    public int size() {
      return list.size();
    }

    @Override
    public boolean add(X x) {
      int pos = list.indexOf(x);
      if (pos == -1) {
        list.add(x);
        return true;
      }
      list.set(pos, x);
      return false;
    }

  }

  private class OrderedMap<K, V> extends AbstractMap<K, V> {
    private OrderedSet<Entry<K, V>> entries = new OrderedSet<>();

    @Override
    public Set<Entry<K, V>> entrySet() {
      return entries;
    }

    @Override
    public V put(K key, V value) {
      for (Entry<K, V> e : entries) {
        if (e.getKey().equals(key)) {
          V old = e.getValue();
          e.setValue(value);
          return old;
        }
      }
      entries.add(new SimpleEntry<>(key, value));
      return null;
    }

  }

  private final List<Object> selectObjects = new ArrayList<>();
  private final Map<String, DbClass> fromClasses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private StringBuilder select = new StringBuilder("SELECT ");
  private final Separator selectComma = new Separator();
  private final Map<String, Join> joins = new OrderedMap<>();
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
  @Getter private boolean lastPage = false;
  @Getter private int page = 1;
  @Getter private int rowsPerPage = 20;

  Query(Connection connection, CommandScheme commands, Class<?> queryType) throws SQLException {
    this.commands = commands;
    this.queryType = queryType;
    this.connection = connection;
  }

  public Query<X> rowsPerPage(int rows) {
    rowsPerPage = rows;
    return this;
  }

  public Query<X> page(int page) {
    this.page = page;
    return this;
  }

  private DbClass selectColumns(Class<?> cls, String alias) throws SQLException {
    DbClass dbc = Database.getDbClass(cls);
    return selectColumns(dbc, alias);
  }

  static String getAlias(DbClass type, DbClass reference, String alias) {
    if (type == reference) return alias;
    return alias + type.getLabel();
  }

  private DbClass selectColumns(DbClass dbc, String alias) throws SQLException {
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
      addHierarchyJoins(dbc, alias);
    }
    boolean first = true;
    List<DbClass> hierarchy = dbc.getTypeHierarchy();
    for (DbClass type : hierarchy) {
      String typeAlias = getAlias(type, dbc, alias);
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

  public Query<X> select(Class<?> cls) throws SQLException {
    return select(cls, null);
  }

  public Query<X> select(Class<?> cls, String alias) throws SQLException {
    selectColumns(cls, alias);
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
    DbClass dbc = Database.getDbClass(cls);
    if (alias == null) {
      alias = dbc.getName();
    }
    fromClasses.put(alias, dbc);
    return this;
  }

  public Query<X> join(String path) throws SQLException {
    return join(path, null);
  }

  public Query<X> join(String path, String alias) throws SQLException {
    String[] parts = path.split("\\.");
    String pAlias = parts[0];
    for (int i = 1; i < parts.length - 2; ++i) {
      pAlias = addJoin(pAlias, parts[i], " JOIN ", "x" + (aliasPostfix++));
    }
    addJoin(pAlias, parts[parts.length - 1], " JOIN ", alias);
    return this;
  }

  private void constructJoin(StringBuilder from, String toAlias, Join join) {
    from.append(join.joinType()).append(join.toClass().getName());
    if (!toAlias.equals(join.toClass().getName())) {
      from.append(' ').append(toAlias);
    }
    from.append(" ON ");
    Separator and = new Separator(" AND ");
    List<DbField> fromFields = join.role.getFields();
    String fromAlias = join.alias();
    if (join.role().getEntity() != join.fromClass()) {
      fromAlias = toAlias;
      toAlias = join.alias();
    }
    for (int j = 0; j < fromFields.size(); ++j) {
      DbField field = fromFields.get(j);
      DbField pkField = field;
      if (field instanceof DbForeignKeyField fkField) {
        pkField = fkField.getPrimaryKeyField();
      }
      and.next(from);
      from.append(fromAlias).append('.').append(field.getName()).append('=').append(toAlias)
          .append('.').append(pkField.getName());
    }

  }

  private void saveJoin(String alias, DbClass fromClass, String fromAlias, DbClass toClass,
      DbSearchMethod key, String joinType) throws SQLException {
    if (alias == null) {
      alias = toClass.getName();
    }
    fromClasses.put(alias, toClass);
    joins.put(alias, new Join(fromClass, fromAlias, toClass, key, joinType));
  }

  private void addHierarchyJoins(DbClass target, String targetAlias) throws SQLException {
    for (DbClass c : target.getTypeHierarchy()) {
      if (c != target) {
        String alias = Query.getAlias(c, target, targetAlias);
        String joinType = " JOIN ";
        if (!c.getType().isAssignableFrom(target.getType())) {
          joinType = " LEFT JOIN ";
        }
        saveJoin(alias, target, targetAlias, c, c.getPrimaryKey(), joinType);
      }
    }
  }

  private String addJoin(String className, String memberName, String joinType, String alias)
      throws SQLException {
    DbClass fromClass = fromClasses.get(className);
    if (fromClass == null) {
      throw new SQLException("class name or alias " + className + " not found in query");
    }
    String[] memberParts = memberName.split("\\|");
    memberName = memberParts[0];
    DbRole role = fromClass.getForeignKey(memberName);
    DbSearchMethod foreignKey = null;
    DbClass type = null;
    if (role == null) {
      role = fromClass.getLink(memberName);
      if (role == null) {
        throw new SQLException("Role " + memberName + " not found in class " + fromClass.getName());
      }
      foreignKey = role.getOpposite().getForeignKey();
      type = Database.getDbClass(role.getType());
      saveJoin(alias, fromClass, className, type, foreignKey, joinType);
    } else {
      type = Database.getDbClass(role.getType());
      foreignKey = role.getForeignKey();
      saveJoin(alias, fromClass, className, type, foreignKey, joinType);
    }
    if (memberParts.length == 2) {
      DbClass subType = type.getSubclass(memberParts[1]);
      String newAlias = alias + subType.getLabel();
      saveJoin(newAlias, type, alias, subType, subType.getPrimaryKey(), " JOIN ");
      alias = newAlias;
    } else if (memberParts.length > 2) {
      throw new SQLException("More than one cast '|' sign in query :" + memberParts);
    }
    return alias;
  }

  private Object addParameter(String cls, String member) throws SQLException {
    DbClass dbc = fromClasses.get(cls);
    DbField field = dbc.getField(member);
    if (field != null) {
      setters.add(commands.getStmtSetter(field.getType()));
      return field;
    }
    DbRole role = dbc.getForeignKey(member);
    if (role == null) {
      role = dbc.getLink(member);
    }
    if (role != null) {
      for (DbField f : role.getForeignKey().getFields()) {
        setters.add(commands.getStmtSetter(f.getType()));
      }
    } else {
      throw new SQLException("Member " + member + " not defined in class " + dbc.getName());
    }
    return role;
  }

  public Query<X> where(String path, String operator, Object value) throws SQLException {
    return addCondition(path, " WHERE ", operator, value);
  }

  private Query<X> addCondition(String path, String initialSeparator, String operator, Object value)
      throws SQLException {
    String[] parts = path.split("\\.");
    String alias = parts[0];
    for (int i = 1; i < parts.length - 1; ++i) {
      alias = addJoin(alias, parts[i], " JOIN ", "x" + (aliasPostfix++));
    }
    String memberName = parts[parts.length - 1];
    Object member = addParameter(alias, memberName);
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
        where.append(alias);
        where.append('.');
        where.append(field.getName());
        where.append(operator);
        where.append('?');

        Object key = pkField.get(value);
        values.add(key);
      }
    } else if (member instanceof DbField) {
      where.append(initialSeparator).append(alias).append('.').append(memberName).append(operator)
          .append('?');
      values.add(value);
    }
    return this;
  }

  public Query<X> and(String path, String operator, Object value) throws SQLException {
    return addCondition(path, " AND ", operator, value);
  }

  public Query<X> or(String path, String operator, Object value) throws SQLException {
    return addCondition(path, " OR ", operator, value);
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

  private StringBuilder buildFrom() {
    StringBuilder from = new StringBuilder(" FROM ");
    TreeSet<String> froms = new TreeSet<>(fromClasses.keySet());
    froms.removeAll(joins.keySet());
    Separator comma = new Separator();
    for (String alias : froms) {
      DbClass dbc = fromClasses.get(alias);
      comma.next(from);
      from.append(dbc.getName());
      if (!alias.equals(dbc.getName())) {
        from.append(" ").append(alias);
      }
    }
    for (Entry<String, Join> e : joins.entrySet()) {
      constructJoin(from, e.getKey(), e.getValue());
    }
    return from;
  }

  private StringBuilder getSql() {
    StringBuilder from = buildFrom();
    StringBuilder sql = new StringBuilder(select).append(from).append(where);
    commands.addSelectPage(sql);
    return sql;
  }

  private int getOffset() {
    return (page - 1) * rowsPerPage;
  }

  private void execute() throws SQLException {
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
      return true;
    }
    return false;
  }

  public void firstPage() throws SQLException {
    page = 1;
    execute();
  }

  public X next() throws SQLException {
    if (resultSet == null) {
      execute();
    }
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
          row.add(commands.getRsGetter(cls).get(resultSet, index++));
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
    firstPage();
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

  @Override
  public void close() throws IOException {
    try {
      if (resultSet != null) {
        resultSet.close();
      }
    } catch (SQLException ex) {
      throw new IOException(ex);
    }
  }

}
