package com.palisand.bones.persist;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.palisand.bones.persist.CommandScheme.Separator;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbField;
import com.palisand.bones.persist.Database.DbForeignKeyField;
import com.palisand.bones.persist.Database.DbRole;
import com.palisand.bones.persist.Database.DbSearchMethod;
import com.palisand.bones.persist.Database.StmtSetter;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represent one database query
 * 
 * @param <X> either a record type, a persistent class or a simple type
 */
public class Query<X> implements Closeable {

  private record Join(DbClass fromClass, String alias, DbClass toClass, DbSearchMethod role,
      String joinType) {
  }

  private class OrderedSet<Y> extends AbstractSet<Y> {
    private final ArrayList<Y> list = new ArrayList<>();

    @Override
    public Iterator<Y> iterator() {
      return list.iterator();
    }

    @Override
    public int size() {
      return list.size();
    }

    @Override
    public boolean add(Y x) {
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
  private final List<String> selectAliases = new ArrayList<>();
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
  @Getter
  private boolean lastPage = false;
  @Getter
  @Setter
  private int page = 1;
  @Getter
  private int rowsPerPage = 20;

  Query(Connection connection, CommandScheme commands, Class<?> queryType) throws SQLException {
    this.commands = commands;
    this.queryType = queryType;
    this.connection = connection;
  }

  /**
   * Sets the number of rows in a page. The query will not return more rows than indicated here. The
   * default is 20
   * 
   * @param rows the maximum number of rows returned per page.
   * @return the query object
   */
  public Query<X> rowsPerPage(int rows) {
    rowsPerPage = rows;
    return this;
  }

  /**
   * Set the page number it should return. The default is 1 (the first page). 0 (zero) or lower is
   * not a valid value.
   * 
   * @param page 1 or higher to indicate the page to return
   * @return the query object
   */
  public Query<X> page(int page) {
    this.page = page;
    return this;
  }

  private DbClass selectColumns(Class<?> cls, String alias) throws SQLException {
    DbClass dbc = Database.getDbClass(cls);
    return selectColumns(dbc, alias);
  }

  static String getAlias(DbClass type, DbClass reference, String alias) {
    if (type == reference) {
      return alias;
    }
    return alias + type.getLabel();
  }

  private DbClass selectColumns(DbClass dbc, String alias) throws SQLException {
    if (alias == null) {
      alias = dbc.getName();
    }
    selectObjects.add(dbc);
    selectAliases.add(alias);
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

  Query<X> select(Class<?> cls) throws SQLException {
    return select(cls, cls.getSimpleName());
  }

  Query<X> select(Class<?> cls, String alias) throws SQLException {
    selectColumns(cls, alias);
    return this;
  }

  /**
   * With this method you can specify specific columns to select. Use this to select expressions
   * like sum(..) or count(..). The columns should be in order of the field of the record specified
   * as type of the query when you call Database.newQuery. When you do not use a record then only
   * one column may be specified.
   * 
   * @param columns the column specification in valid SQL
   * @return the query object
   * @throws SQLException
   */
  public Query<X> select(String... columns) throws SQLException {
    for (String column : columns) {
      String newColumn = addSelectObject(column, selectObjects.size());
      selectComma.next(select);
      select.append(newColumn);
    }
    return this;
  }

  private String makeColumn(String orig, String name) {
    String as = " AS ";
    int pos = orig.toUpperCase().indexOf(as);
    StringBuilder sb = new StringBuilder(orig);
    if (pos == -1) {
      sb.append(as);
      sb.append(name);
    } else {
      sb.replace(pos + as.length(), sb.length(), name);
    }
    return sb.toString();
  }

  private String addSelectObject(String column, int position) throws SQLException {
    if (queryType.isRecord()) {
      RecordComponent rc = queryType.getRecordComponents()[position];
      selectObjects.add(rc.getType());
      selectAliases.add(rc.getName());
      return makeColumn(column, rc.getName());
    }
    if (position == 0) {
      selectObjects.add(queryType);
      selectAliases.add("result");
      return makeColumn(column, "result");
    }
    throw new SQLException("queryType is not a record and more then one rows are selected");
  }

  /**
   * With this method you can specify extra tables in the from clause of the query. You only need to
   * call this method when you use the {@link com.palisand.bones.persist.Query#select} method. Since
   * this method will not create any joins, you should use this only when a normal join does not
   * work for you or otherwise use {@link com.palisand.bones.persist.Query#join} or
   * {@link com.palisand.bones.persist.Query#selectJoin}. This method will you the name of the Class
   * as an implicit alias for the class.
   * 
   * @param cls the persistent class you want to select from
   * @return the query object
   * @throws SQLException
   */
  public Query<X> from(Class<?> cls) throws SQLException {
    return from(cls, cls.getSimpleName());
  }

  /**
   * With this method you can specify extra tables in the from clause of the query. You only need to
   * call this method when you use the {@link com.palisand.bones.persist.Query#select} method. In
   * this variant you can give an explicit alias for the class.
   * 
   * @param cls the persistent class you want to select from
   * @return the query object
   * @throws SQLException
   */
  public Query<X> from(Class<?> cls, String alias) throws SQLException {
    if (alias == null) {
      throw new NullPointerException("alias should not be null");
    }
    DbClass dbc = Database.getDbClass(cls);
    fromClasses.put(alias, dbc);
    return this;
  }

  /**
   * specify a join for the query. You should start the path parameter with an alias or class name
   * and use .&lt;relation&gt; to specify the foreign key to use. You can use multiple joins in the
   * path You can cast the result of a join to a subtype by using the pipe symbol (|). The path
   * should always start with a hash (#)
   * 
   * @param path the path of relations and casts that the join should follow
   * @param alias the alias the resulting class should use, may not be null
   * @return the query object
   * @throws SQLException
   */
  public Query<X> join(String path, String alias) throws SQLException {
    if (alias == null) {
      throw new NullPointerException("alias should not be null");
    }
    joinReturnsAlias(path, alias);
    return this;
  }

  private String joinReturnsAlias(String path, String alias) throws SQLException {
    if (!path.startsWith("#")) {
      throw new SQLException("path " + path + " does not start with a #");
    }
    String[] parts = path.substring(1).split("\\.");
    String pAlias = parts[0];
    for (int i = 1; i < parts.length - 2; ++i) {
      pAlias = addJoin(pAlias, parts[i], " LEFT OUTER JOIN ", "x" + (aliasPostfix++));
    }
    return addJoin(pAlias, parts[parts.length - 1], " LEFT OUTER JOIN ", alias);
  }

  /**
   * Adds the join that the path specifies and add the class to the implicit selection results.
   * selected objects of this query will have the relation specified by the path set, so a call to
   * get<Relation> of the selected object will retrieve the related object with all its property
   * values. This method will use the class name as an implicit alias.
   * 
   * @param path parameter specifing the relation object that should be selected
   * @param cls the type of the selected object. It should be the type the path leads to.
   * @return the query itself
   * @throws SQLException
   */
  public Query<X> selectJoin(String path, Class<?> cls) throws SQLException {
    return selectJoin(path, cls, cls.getSimpleName());
  }

  /**
   * Adds the join that the path specifies and add the class to the implicit selection results.
   * selected objects of this query will have the relation specified by the path set, so a call to
   * get<Relation> of the selected object will retrieve the related object with all its property
   * values.
   * 
   * @param path: parameter specifing the relation object that should be selected
   * @param cls: the type of the selected object. It should be the type the path leads to.
   * @param alias: the alias for the table the relation in the path leads to.
   * @return the query itself
   * @throws SQLException
   */
  public Query<X> selectJoin(String path, Class<?> cls, String alias) throws SQLException {
    String pAlias = joinReturnsAlias(path, alias);
    select(cls, pAlias);
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

  /**
   * Method to add a where clause to a query. The asql string contains a enhanced sql clause. This
   * clause differs from normal that you can use implicit joins by using java like navigation over
   * roles like #alias.role1.role2 etc. You need to add the #-sign in front of this path
   * specifications. A path may also contain a cast to subclass like this:
   * #alias.role|Subclass.fieldOfSubclass. You can use parameters by adding a questionmark (?) to
   * the query and add the value in the values parameter. This should be done in the order they
   * appear in the clause
   * 
   * @param asql: the enhanced sql string
   * @param values: the values that will be set as parameters in the resulted preparedstatement
   * @return the query
   * @throws SQLException
   */
  public Query<X> where(String asql, Object... values) throws SQLException {
    where.append(" WHERE ");
    addCondition(asql, values);
    return this;
  }

  /**
   * Method to add a having clause to a query. The asql string contains a enhanced sql clause. This
   * clause differs from normal that you can use implicit joins by using java like navigation over
   * roles like #alias.role1.role2 etc. You need to add the #-sign in front of this path
   * specifications. A path may also contain a cast to subclass like this:
   * #alias.role|Subclass.fieldOfSubclass. You can use parameters by adding a questionmark (?) to
   * the query and add the value in the values parameter. This should be done in the order they
   * appear in the clause
   * 
   * @param asql: the enhanced sql string
   * @param values: the values that will be set as parameters in the resulted preparedstatement
   * @return the query
   * @throws SQLException
   */
  public Query<X> having(String asql, Object... values) throws SQLException {
    where.append(" HAVING ");
    addCondition(asql, values);
    return this;
  }

  private static final Pattern TOKENS = Pattern.compile("\\?|#[a-zA-Z0-9_\\.\\|]+");

  private void addCondition(String asql, Object... values) throws SQLException {
    Matcher matcher = TOKENS.matcher(asql);
    int pos = 0;
    int valueIndex = 0;
    while (matcher.find()) {
      where.append(asql.substring(pos, matcher.start()));
      switch (matcher.group().charAt(0)) {
        case '?':
          handleValue(values[valueIndex++]);
          break;
        case '#':
          handlePath(matcher.group());
          break;
      }
      pos = matcher.end();
    }
    where.append(asql.substring(pos));
  }

  private void handleValue(Object value) throws SQLException {
    where.append('?');
    if (value != null) {
      DbClass dbc = Database.getDbClass(value.getClass());
      if (dbc == null) {
        values.add(value);
        setters.add(commands.getStmtSetter(value.getClass()));
      } else {
        for (DbField field : dbc.getPrimaryKey().getFields()) {
          Object key = field.get(value);
          values.add(key);
          setters.add(commands.getStmtSetter(field.getType()));
        }
      }
    } else {
      values.add(value);
      setters.add(commands.getStmtSetter(Object.class));
    }
  }

  private Object addParameterNew(String cls, String member) throws SQLException {
    DbClass dbc = fromClasses.get(cls);
    DbField field = dbc.getField(member);
    if (field != null) {
      return field;
    }
    DbRole role = dbc.getForeignKey(member);
    if (role == null) {
      role = dbc.getLink(member);
    }
    return role;
  }

  private void handlePath(String path) throws SQLException {
    String[] parts = path.substring(1).split("\\.");
    String alias = parts[0];
    for (int i = 1; i < parts.length - 1; ++i) {
      alias = addJoin(alias, parts[i], " JOIN ", "x" + (aliasPostfix++));
    }
    String memberName = parts[parts.length - 1];
    Object member = addParameterNew(alias, memberName);
    if (member instanceof DbRole role) {
      if (!role.isForeignKey()) {
        role = role.getOpposite();
        // TODO: add join
      }
      Separator sep = new Separator(" AND ");
      DbSearchMethod foreignKey = role.getForeignKey();
      for (int i = 0; i < foreignKey.getFields().size(); ++i) {
        DbField field = foreignKey.getFields().get(i);
        sep.next(where);
        where.append(alias);
        where.append('.');
        where.append(field.getName());
      }
    } else if (member instanceof DbField) {
      where.append(alias).append('.').append(memberName);
    }
  }

  /**
   * specifies the order by clause of the query. Use the correct aliasses and fieldnames. When you
   * use a record as the type of the query the names of the fields in the record are used as field
   * indicators and may also be used in the order by parameter. Use a comma (,) between columns. All
   * queries should have a orderBy specified because it is obligatory in MS SqlServer when used with
   * paging.
   * 
   * @param orderBy a list of fields
   * @return the query object
   */
  public Query<X> orderBy(String orderBy) {
    where.append(" ORDER BY ").append(orderBy);
    return this;
  }

  /**
   * specifies the columns in a group by clause. Use the correct format as in
   * {@link com.palisand.bones.persist.Query.orderBy}
   * 
   * @param columns: The columns to include in
   * @return
   */
  public Query<X> groupBy(String columns) {
    where.append(" GROUP BY ").append(columns);
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
    for (Entry<String, Join> e = getNextJoin(froms); e != null; e = getNextJoin(froms)) {
      constructJoin(from, e.getKey(), e.getValue());
    }
    return from;
  }

  private Entry<String, Join> getNextJoin(Set<String> aliases) {
    for (Entry<String, Join> e : joins.entrySet()) {
      if (aliases.contains(e.getValue().alias()) && !aliases.contains(e.getKey())) {
        aliases.add(e.getKey());
        return e;
      }
    }
    return null;
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
    if (resultSet != null) {
      resultSet.close();
    }
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
  private X mapRow(Map<String, Object> row) throws SQLException {
    X result = null;
    TreeSet<String> froms = new TreeSet<>(fromClasses.keySet());
    froms.removeAll(joins.keySet());
    if (queryType.isRecord()) {
      try {
        Object[] parameters = new Object[row.size()];
        int i = 0;
        for (String alias : selectAliases) {
          parameters[i++] = row.get(alias);
        }
        result = (X) queryType.getDeclaredConstructors()[0].newInstance(parameters);
      } catch (Exception ex) {
        if (ex.getCause() != null) {
          throw new SQLException(ex.getCause());
        }
        throw new SQLException(ex);
      }
    } else if (queryType.isInstance(row.get(froms.first()))) {
      result = (X) row.get(froms.first());
    }
    linkObjects(froms, row);
    return result;
  }

  private void linkObjects(TreeSet<String> froms, Map<String, Object> row) throws SQLException {
    for (Entry<String, Join> e = getNextJoin(froms); e != null; e = getNextJoin(froms)) {
      String[] parts = e.getValue().role().getName().split("_");
      if (parts.length == 2) {
        Object parent = row.get(e.getValue().alias());
        if (parent != null) {
          Object child = row.get(e.getKey());
          if (child != null) {
            DbClass cls = fromClasses.get(e.getValue().alias());
            DbRole field = cls.getForeignKey(parts[1]);
            field.set(parent, child);
          }
        }
      }
    }
  }

  /**
   * executes the query for a next page to retrieve
   * 
   * @return
   * @throws SQLException
   */
  public boolean nextPage() throws SQLException {
    if (!isLastPage()) {
      ++page;
      execute();
      return true;
    }
    return false;
  }

  /**
   * return to the first page after you have selected other pages. It is not necessary to use this
   * method when you start iterating over the results with next();
   * 
   * @throws SQLException
   */
  public void firstPage() throws SQLException {
    page = 1;
    execute();
  }

  /**
   * get the next instance from the results of this query
   * 
   * @return
   * @throws SQLException
   */
  public X next() throws SQLException {
    if (resultSet == null) {
      execute();
    }
    if (resultSet.next()) {
      Map<String, Object> row = new TreeMap<>();
      int index = 1;
      int objectIndex = 0;
      for (Object obj : selectObjects) {
        if (obj instanceof DbClass cls) {
          String label = null;
          DbClass realCls = cls;
          Boolean isNull = null;
          if (cls.getRoot().hasSubTypeField()) {
            label = resultSet.getString(index++);
            if (resultSet.wasNull()) {
              isNull = true;
            } else {
              realCls = cls.getLabel().equals(label) ? cls : cls.getSubClasses().get(label);
              isNull = false;
            }
          }
          if ((isNull != null && isNull == true) || isNull(resultSet, cls, index)) {
            row.put(selectAliases.get(objectIndex), null);
          } else {
            Object result = realCls.newInstance();
            index = commands.setPrimaryKey(resultSet, cls, result, index);
            result = commands.cache(cls, result);
            index = commands.setHierarchyValues(resultSet, cls, realCls, result, index);
            row.put(selectAliases.get(objectIndex), result);
          }
        } else if (obj instanceof Class<?> cls) {
          row.put(selectAliases.get(objectIndex),
              commands.getRsGetter(cls).get(resultSet, index++));
        }
        ++objectIndex;
      }
      ++rowInPage;
      return mapRow(row);
    }
    lastPage = rowInPage < rowsPerPage;
    return null;
  }

  private boolean isNull(ResultSet resultSet, DbClass cls, int index) throws SQLException {
    cls.getPrimaryKey().getFields().get(0).rsGet(resultSet, index);
    return resultSet.wasNull();
  }

  /**
   * returns a list with all results of all pages this query This method should be used with
   * caution, because all pages are selected.
   * 
   * @return a list with all results
   * @throws SQLException
   */
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

  /**
   * returns a list with a results of the current page
   * 
   * @return list with all instances in this page
   * @throws SQLException
   */
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

  /**
   * close method to release the result set and other resources associated with this query
   */
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
