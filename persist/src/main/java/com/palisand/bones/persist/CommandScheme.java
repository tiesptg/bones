package com.palisand.bones.persist;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.palisand.bones.persist.CommandScheme.Metadata.DbIndex;
import com.palisand.bones.persist.CommandScheme.Metadata.DbTable;
import com.palisand.bones.persist.Database.DbClass;
import com.palisand.bones.persist.Database.DbClass.DbField;
import com.palisand.bones.persist.Database.DbClass.DbRole;
import com.palisand.bones.persist.Database.DbClass.DbSearchMethod;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class CommandScheme {
	private Map<Class<?>, JDBCType> typeMap = new HashMap<>();
	protected static final String FOREIGN_KEY_PREFIX = "fk_";
	protected static final String INDEX_PREFIX = "idx_";
	protected static final String SUBTYPE_FIELD = "subtype";
	protected Map<DbClass, Statements> statements = new HashMap<>();
	private Consumer<String> logger = null;
	private final Map<String, Map<String, Object>> cache = new TreeMap<>();
	private boolean indexForFkNeeded = true;
	@Getter
	protected final Map<String, PreparedStatement> queryCache = new ConcurrentHashMap<>();

	@Getter
	@Setter
	static private class Statements {
		private PreparedStatement insert;
		private String insertSql;
		private PreparedStatement update;
		private String updateSql;
		private PreparedStatement delete;
		private String deleteSql;
		private PreparedStatement refresh;
		private String refreshSql;
	}

	@Data
	static class Metadata {

		@Data
		static class DbTable {
			private final String name;
			private final Map<String, DbColumn> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			private DbIndex primaryKey;
			private final Map<String, DbIndex> foreignKeys = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			private final Map<String, DbIndex> indices = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		}

		@Data
		static class DbColumn {
			private final String name;
			private final JDBCType type;
			private final boolean nullable;
		}

		@Data
		static class DbIndex {
			private final String name;
			private final boolean unique;
			private DbTable references = null;
			private final List<DbColumn> fields = new ArrayList<>();
		}

		private final Map<String, DbTable> tables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		private String catalog;
	}

	Metadata getMetadata(Connection connection) throws SQLException {
		System.out.println("get metadata");
		DatabaseMetaData dbmd = connection.getMetaData();
		Metadata metadata = new Metadata();
		ResultSet tables = dbmd.getTables(connection.getCatalog(), connection.getSchema(), null, new String[] { "TABLE" });
		while (tables.next()) {
			DbTable table = new DbTable(tables.getString("TABLE_NAME"));
			metadata.getTables().put(table.getName(), table);
			ResultSet fields = dbmd.getColumns(connection.getCatalog(), connection.getSchema(), table.getName(), null);
			while (fields.next()) {
				Metadata.DbColumn field = new Metadata.DbColumn(fields.getString("COLUMN_NAME"),
						JDBCType.valueOf(fields.getInt("DATA_TYPE")), fields.getBoolean("IS_NULLABLE"));
				table.getFields().put(field.getName(), field);
			}
			ResultSet pkeys = dbmd.getPrimaryKeys(connection.getCatalog(), connection.getSchema(), table.getName());
			DbIndex pkey = new DbIndex("pk", true);
			table.setPrimaryKey(pkey);
			while (pkeys.next()) {
				pkey.getFields().add(table.getFields().get(pkeys.getString("COLUMN_NAME")));
			}
			ResultSet indices = dbmd.getIndexInfo(connection.getCatalog(), connection.getSchema(), table.getName(), false,
					false);
			DbIndex index = null;
			while (indices.next()) {
				String name = indices.getString("INDEX_NAME");
				if (index == null || !index.getName().equalsIgnoreCase(name)) {
					index = new DbIndex(name, !indices.getBoolean("NON_UNIQUE"));
					table.getIndices().put(name, index);
				}
				index.getFields().add(table.getFields().get(indices.getString("COLUMN_NAME")));
			}
			String key = null;
			for (Entry<String, DbIndex> entry : table.getIndices().entrySet()) {
				if (entry.getValue().getFields().equals(table.getPrimaryKey().getFields())) {
					key = entry.getKey();
					break;
				}
			}
			if (key != null) {
				table.getIndices().remove(key);
			}
		}
		Collection<DbTable> found = metadata.getTables().values();
		for (DbTable table : found) {
			for (DbTable other : found) {
				ResultSet fkeys = dbmd.getCrossReference(connection.getCatalog(), connection.getSchema(), other.getName(),
						connection.getCatalog(), connection.getSchema(), table.getName());
				Metadata.DbIndex fkey = null;
				while (fkeys.next()) {
					String name = fkeys.getString("FK_NAME");
					if (fkey == null || !fkey.getName().equals(name)) {
						fkey = new DbIndex(name, false);
						table.getForeignKeys().put(name, fkey);
						fkey.setReferences(other);
					}
					fkey.getFields().add(table.getFields().get(fkeys.getString("FKCOLUMN_NAME")));
				}
			}
		}
		return metadata;
	}

	static class Separator {
		final String firstToken;
		final String token;
		boolean first = true;

		Separator() {
			firstToken = "";
			token = ",";
		}

		Separator(String token) {
			firstToken = "";
			this.token = token;
		}

		Separator(String token, String firstToken) {
			this.firstToken = firstToken;
			this.token = token;
		}

		public void next(StringBuilder sb) {
			if (first) {
				first = false;
				sb.append(firstToken);
			} else {
				sb.append(token);
			}
		}
	}

	public CommandScheme() {
		typeMap.put(short.class, JDBCType.SMALLINT);
		typeMap.put(Short.class, JDBCType.SMALLINT);
		typeMap.put(int.class, JDBCType.INTEGER);
		typeMap.put(Integer.class, JDBCType.INTEGER);
		typeMap.put(long.class, JDBCType.BIGINT);
		typeMap.put(Long.class, JDBCType.BIGINT);
		typeMap.put(boolean.class, JDBCType.BOOLEAN);
		typeMap.put(Boolean.class, JDBCType.BOOLEAN);
		typeMap.put(String.class, JDBCType.VARCHAR);
		typeMap.put(OffsetDateTime.class, JDBCType.TIME_WITH_TIMEZONE);
		typeMap.put(LocalDate.class, JDBCType.DATE);
		typeMap.put(BigDecimal.class, JDBCType.DECIMAL);
		typeMap.put(double.class, JDBCType.DOUBLE);
		typeMap.put(Double.class, JDBCType.DOUBLE);
		typeMap.put(float.class, JDBCType.REAL);
		typeMap.put(Float.class, JDBCType.REAL);
	}

	public CommandScheme logger(Consumer<String> logger) {
		this.logger = logger;
		return this;
	}

	public Consumer<String> getLogger() {
		return logger;
	}

	protected void log(String str) {
		this.logger.accept(str);
	}

	public CommandScheme indexForFkNeeded(boolean value) {
		indexForFkNeeded = value;
		return this;
	}

	protected boolean isIndexForFkNeeded() {
		return indexForFkNeeded;
	}

	protected Object cache(DbClass entity, Object object) throws SQLException {
		entity = entity.getRoot();
		Map<String, Object> typeCache = cache.get(entity.getName());
		if (typeCache == null) {
			typeCache = new TreeMap<>();
			cache.put(entity.getName(), typeCache);
		} else {
			Object result = typeCache.get(entity.getPrimaryKeyAsString(object));
			if (result != null) {
				return result;
			}
		}
		typeCache.put(entity.getPrimaryKeyAsString(object), object);
		return object;
	}

	protected void replaceInCache(DbClass entity, Object object) throws SQLException {
		entity = entity.getRoot();
		Map<String, Object> typeCache = cache.get(entity.getName());
		typeCache.put(entity.getPrimaryKeyAsString(object), object);
	}

	protected Object removeFromCache(DbClass entity, Object object) throws SQLException {
		entity = entity.getRoot();
		Map<String, Object> typeCache = cache.get(entity.getName());
		if (typeCache != null) {
			Object result = typeCache.remove(entity.getPrimaryKeyAsString(object));
			if (result != null) {
				return result;
			}
		}
		return object;
	}

	public void clearCache() {
		cache.clear();
	}

	protected String typeName(JDBCType type) {
		return type.getName();
	}

	protected boolean execute(Connection connection, String sql) throws SQLException {
		log(sql);
		return connection.createStatement().execute(sql);
	}

	private JDBCType getJDBCType(Class<?> cls) throws SQLException {
		JDBCType type = typeMap.get(cls);
		if (type == null) {
			throw new SQLException("Type " + cls + " is not (yet) supported");
		}
		return type;
	}

	private String getType(DbField attribute) throws SQLException {
		JDBCType type = getJDBCType(attribute.getType());
		return typeName(type);
	}

	private void appendColumn(StringBuilder sql, String prefix, DbField attribute, boolean nullable) throws SQLException {
		if (!prefix.isEmpty()) {
			sql.append(prefix);
			sql.append('_');
		}
		appendColumn(sql, attribute, nullable);
	}

	private void appendColumn(StringBuilder sql, DbField attribute, boolean nullable) throws SQLException {
		sql.append(attribute.getName());
		sql.append(" ");
		sql.append(getType(attribute));
		if (!nullable) {
			sql.append(" NOT");
		}
		sql.append(" NULL");
	}

	protected void upgradeColumns(Connection connection, DbTable dbTable, DbClass entity) throws SQLException {
		TreeSet<String> fieldsToRemove = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		fieldsToRemove.addAll(dbTable.getFields().keySet());
		if (entity.hasSubTypeField()) {
			if (!fieldsToRemove.remove(SUBTYPE_FIELD)) {
				addFieldToTable(connection, entity, SUBTYPE_FIELD + " VARCHAR(4) NOT NULL");
			}
		}
		for (DbField attribute : entity.getFields()) {
			if (!fieldsToRemove.remove(attribute.getName())) {
				addFieldToTable(connection, entity, attribute);
			}
		}
		for (DbRole role : entity.getForeignKeys()) {
			for (DbField attribute : role.getForeignKey().getFields()) {
				if (!fieldsToRemove.remove(attribute.getName())) {
					addFieldToTable(connection, entity, attribute);
				}
			}
		}
		for (String name : fieldsToRemove) {
			removeFieldFromTable(connection, entity, name);
		}
	}

	protected void upgradeIndices(Connection connection, DbTable dbTable, DbClass entity) throws SQLException {
		TreeSet<String> indicesToRemove = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		indicesToRemove.addAll(dbTable.getIndices().keySet());
		for (DbSearchMethod index : entity.getIndices().values()) {
			if (!indicesToRemove.remove(INDEX_PREFIX + index.getName())) {
				createIndex(connection, index);
			}
		}
		for (DbRole role : entity.getForeignKeys()) {
			DbSearchMethod index = role.getForeignKey();
			if (!indicesToRemove.remove(INDEX_PREFIX + index.getName())) {
				if (isIndexForFkNeeded()) {
					createIndex(connection, index);
				}
			}
		}
		for (String name : indicesToRemove) {
			if (isIndexForFkNeeded()) {
				dropIndex(connection, entity, name);
			}
		}
	}

	protected void upgradeTable(Connection connection, DbTable dbTable, DbClass entity) throws SQLException {
		if (dbTable == null) {
			createTable(connection, entity);
		} else {
			upgradeColumns(connection, dbTable, entity);
			upgradeIndices(connection, dbTable, entity);
		}
	}

	public void upgradeForeignKey(Connection connection, DbTable table, DbRole role) throws SQLException {
		DbIndex fk = null;
		if (table != null) {
			fk = table.getForeignKeys().get(FOREIGN_KEY_PREFIX + role.getForeignKey().getName());
		}
		if (fk == null) {
			createForeignKey(connection, role);
		}
	}

	protected void removeFieldFromTable(Connection connection, DbClass entity, String columnName) throws SQLException {
		StringBuilder sql = new StringBuilder("ALTER TABLE ");
		sql.append(entity.getName());
		sql.append(" DROP COLUMN ");
		sql.append(columnName);
		execute(connection, sql.toString());
	}

	protected void addFieldToTable(Connection connection, DbClass entity, DbField attribute) throws SQLException {
		StringBuilder sql = new StringBuilder();
		appendColumn(sql, attribute, attribute.isNullable());
		addFieldToTable(connection, entity, sql.toString());
	}

	protected void addFieldToTable(Connection connection, DbClass entity, String column) throws SQLException {
		StringBuilder sql = new StringBuilder("ALTER TABLE ");
		sql.append(entity.getName());
		sql.append(" ADD ");
		sql.append(column);
		execute(connection, sql.toString());
	}

	public void createTable(Connection connection, DbClass entity) throws SQLException {
		StringBuilder sql = new StringBuilder("CREATE TABLE ");
		sql.append(entity.getName());
		sql.append("(");
		for (DbField attribute : entity.getFields()) {
			appendColumn(sql, attribute, attribute.isNullable());
			if (attribute.getId() != null && attribute.isGenerated() && entity.getSuperClass() == null) {
				sql.append(" GENERATED ALWAYS AS IDENTITY");
			}
			sql.append(",");
		}
		if (entity.getSuperClass() == null && !entity.getSubClasses().isEmpty()) {
			sql.append(SUBTYPE_FIELD);
			sql.append(" VARCHAR(4) NOT NULL,");
		}
		for (DbRole role : entity.getForeignKeys()) {
			for (DbField field : role.getForeignKey().getFields()) {
				appendColumn(sql, field, true);
				sql.append(",");
			}
		}
		sql.append("PRIMARY KEY(");
		sql.append(entity.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
		sql.append("))");
		execute(connection, sql.toString());
		for (DbRole role : entity.getForeignKeys()) {
			createIndex(connection, role.getForeignKey());
		}
		for (DbSearchMethod method : entity.getIndices().values()) {
			createIndex(connection, method);
		}
	}

	public void dropTable(Connection connection, String name) throws SQLException {
		StringBuilder sql = new StringBuilder("DROP TABLE ");
		sql.append(name);
		execute(connection, sql.toString());
	}

	protected void dropIndex(Connection connection, DbClass entity, String indexName) throws SQLException {
		StringBuilder sql = new StringBuilder("DROP INDEX IF EXISTS ");
		sql.append(indexName);
		execute(connection, sql.toString());
	}

	public void dropContraint(Connection connection, String tableName, String constraintName) throws SQLException {
		StringBuilder sql = new StringBuilder("ALTER TABLE ");
		sql.append(tableName);
		sql.append(" DROP CONSTRAINT IF EXISTS ");
		sql.append(constraintName);
		execute(connection, sql.toString());
	}

	public void createForeignKey(Connection connection, DbRole role) throws SQLException {
		StringBuilder sql = new StringBuilder("ALTER TABLE ");
		sql.append(role.getEntity().getName());
		sql.append(" ADD CONSTRAINT ");
		sql.append(FOREIGN_KEY_PREFIX);
		DbClass fk = Database.getDbClass(role.getType());
		sql.append(role.getEntity().getName());
		sql.append('_');
		sql.append(role.getName());
		sql.append(" FOREIGN KEY(");
		sql.append(role.getForeignKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
		sql.append(") REFERENCES ");
		sql.append(fk.getName());
		sql.append("(");
		sql.append(fk.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
		sql.append(")");
		execute(connection, sql.toString());
	}

	protected Statements getInsertStatement(Connection connection, DbClass entity) throws SQLException {
		Statements stmts = statements.computeIfAbsent(entity, e -> new Statements());
		if (stmts.getInsert() == null) {
			StringBuilder sql = new StringBuilder("INSERT INTO ");
			StringBuilder params = new StringBuilder();
			sql.append(entity.getName());
			sql.append("(");
			Separator sqlComma = new Separator();
			Separator pComma = new Separator();
			if (entity.hasSubTypeField()) {
				sqlComma.next(sql);
				pComma.next(params);
				sql.append(SUBTYPE_FIELD);
				params.append('?');
			}
			for (DbField field : entity.getFields()) {
				if (!field.isGenerated() || entity.getSuperClass() != null) {
					sqlComma.next(sql);
					pComma.next(params);
					sql.append(field.getName());
					params.append("?");
				}
			}
			for (DbRole role : entity.getForeignKeys()) {
				for (DbField field : role.getForeignKey().getFields()) {
					sqlComma.next(sql);
					pComma.next(params);
					sql.append(field.getName());
					params.append("?");
				}
			}
			sql.append(") VALUES (");
			sql.append(params);
			sql.append(")");
			stmts.setInsert(connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS));
			stmts.setInsertSql(sql.toString());
		}
		return stmts;
	}

	protected Statements getUpdateStatement(Connection connection, DbClass entity) throws SQLException {
		Statements stmts = statements.computeIfAbsent(entity, e -> new Statements());
		if (stmts.getUpdate() == null) {
			StringBuilder sql = new StringBuilder("UPDATE ");
			sql.append(entity.getName());
			sql.append(" SET ");
			Separator sqlComma = new Separator();
			for (DbField field : entity.getFields()) {
				if (!entity.getPrimaryKey().getFields().contains(field)) {
					sqlComma.next(sql);
					sql.append(field.getName());
					sql.append("=?");
				}
			}
			for (DbRole role : entity.getForeignKeys()) {
				for (DbField field : role.getForeignKey().getFields()) {
					sqlComma.next(sql);
					sql.append(field.getName());
					sql.append("=?");
				}
			}
			sql.append(" WHERE ");
			Separator and = new Separator(" AND ");
			for (DbField field : entity.getPrimaryKey().getFields()) {
				and.next(sql);
				sql.append(field.getName());
				sql.append("=?");
			}
			if (entity.getVersion() != null) {
				and.next(sql);
				sql.append(entity.getVersion().getName());
				sql.append("=?");
			}
			stmts.setUpdate(connection.prepareStatement(sql.toString()));
			stmts.setUpdateSql(sql.toString());
		}
		return stmts;
	}

	protected Statements getDeleteStatement(Connection connection, DbClass entity) throws SQLException {
		Statements stmts = statements.computeIfAbsent(entity, e -> new Statements());
		if (stmts.getDelete() == null) {
			StringBuilder sql = new StringBuilder("DELETE FROM ");
			sql.append(entity.getName());
			sql.append(" WHERE ");
			Separator and = new Separator(" AND ");
			for (DbField field : entity.getPrimaryKey().getFields()) {
				and.next(sql);
				sql.append(field.getName());
				sql.append("=?");
			}
			if (entity.getVersion() != null) {
				and.next(sql);
				sql.append(entity.getVersion().getName());
				sql.append("=?");
			}
			stmts.setDelete(connection.prepareStatement(sql.toString()));
			stmts.setDeleteSql(sql.toString());
		}
		return stmts;
	}

	private String getAlias(Map<String, DbClass> map, DbClass cls) {
		for (Entry<String, DbClass> e : map.entrySet()) {
			if (e.getValue() == cls && !cls.getName().equalsIgnoreCase(e.getKey())) {
				return e.getKey();
			}
		}
		return null;
	}

	void addHierarchyJoins(StringBuilder sql, List<DbClass> hierarchy, DbClass target, Map<String, DbClass> aliasses)
			throws SQLException {
		boolean first = true;
		for (DbClass c : hierarchy) {
			String alias = getAlias(aliasses, c);
			if (!first) {
				if (!c.getType().isAssignableFrom(target.getType())) {
					sql.append(" LEFT JOIN ");
				} else {
					sql.append(" JOIN ");
				}
			}
			sql.append(c.getName());
			if (alias != null) {
				sql.append(' ').append(alias);
			}
			if (first) {
				first = false;
			} else {
				sql.append(" USING(");
				Separator and = new Separator(",");
				for (DbField field : c.getPrimaryKey().getFields()) {
					and.next(sql);
					sql.append(field.getName());
				}
				sql.append(')');
			}
		}

	}

	protected Statements getRefreshStatement(Connection connection, DbClass entity) throws SQLException {
		Statements stmts = statements.computeIfAbsent(entity, e -> new Statements());
		if (stmts.getRefresh() == null) {
			List<DbClass> types = entity.getTypeHierarchy();
			StringBuilder sql = new StringBuilder("SELECT ");
			Separator sqlComma = new Separator();
			if (entity.hasSubTypeField()) {
				sqlComma.next(sql);
				sql.append(entity.getName());
				sql.append('.');
				sql.append(SUBTYPE_FIELD);
			}
			for (DbClass c : types) {
				for (DbField field : c.getFields()) {
					if (!c.getPrimaryKey().getFields().contains(field)) {
						sqlComma.next(sql);
						sql.append(c.getName());
						sql.append('.');
						sql.append(field.getName());
					}
				}
				for (DbRole role : c.getForeignKeys()) {
					for (DbField field : role.getForeignKey().getFields()) {
						sqlComma.next(sql);
						sql.append(c.getName());
						sql.append('.');
						sql.append(field.getName());
					}
				}
			}
			sql.append(" FROM ");
			addHierarchyJoins(sql, types, entity, Collections.emptyMap());
			Separator and = new Separator(" AND ", " WHERE ");
			for (DbField field : entity.getPrimaryKey().getFields()) {
				and.next(sql);
				sql.append(entity.getName());
				sql.append('.');
				sql.append(field.getName());
				sql.append("=?");
			}
			stmts.setRefresh(connection.prepareStatement(sql.toString()));
			stmts.setRefreshSql(sql.toString());
		}
		return stmts;
	}

	static void nextValue(StringBuilder sql, Object value) {
		if (sql != null) {
			String literal = getLiteral(value);
			int pos = sql.indexOf("?");
			sql.replace(pos, pos + 1, literal);
		}
	}

	static String getLiteral(Object value) {
		if (value == null) {
			return "null";
		}
		if (value.getClass() == String.class || value.getClass().getName().startsWith("java.time")) {
			return '\'' + value.toString() + '\'';
		}
		return value.toString();
	}

	public Object insert(Connection connection, DbClass entity, String label, Object object) throws SQLException {
		if (entity.getSuperClass() != null) {
			insert(connection, entity.getSuperClass(), label, object);
		}
		Statements stmts = getInsertStatement(connection, entity);
		PreparedStatement stmt = stmts.getInsert();
		StringBuilder sql = null;
		if (logger != null) {
			sql = new StringBuilder(stmts.getInsertSql());
		}
		int index = 1;
		if (entity.hasSubTypeField()) {
			stmt.setString(index++, label);
			nextValue(sql, label);
		}
		for (DbField field : entity.getFields()) {
			if (!field.isGenerated() || entity.getSuperClass() != null) {
				Object value = field.get(object);
				if (value != null) {
					stmt.setObject(index++, value);
				} else {
					stmt.setNull(index++, getJDBCType(field.getType()).ordinal());
				}
				nextValue(sql, value);
			}
		}
		for (DbRole role : entity.getForeignKeys()) {
			Object child = role.get(object);
			DbClass cls = Database.getDbClass(role.getType());
			for (DbField field : cls.getPrimaryKey().getFields()) {
				Object value = child == null ? null : field.get(child);
				if (value != null) {
					stmt.setObject(index++, value);
				} else {
					stmt.setNull(index++, getJDBCType(field.getType()).ordinal());
				}
				nextValue(sql, value);
			}
		}
		if (logger != null) {
			logger.accept(sql.toString());
		}
		if (stmt.executeUpdate() != 0 && entity.getSuperClass() == null) {
			try (ResultSet keys = stmt.getGeneratedKeys()) {
				if (keys.next()) {
					index = 1;
					for (DbField field : entity.getPrimaryKey().getFields()) {
						if (field.isGenerated()) {
							field.set(object, keys.getObject(index++));
						}
					}
				}
			}
		}
		return cache(entity, object);
	}

	private void copy(DbClass cls, Object dest, Object src) throws SQLException {
		for (DbField field : cls.getFields()) {
			field.set(dest, field.get(src));
		}
		for (DbRole role : cls.getForeignKeys()) {
			role.set(dest, role.get(src));
		}
		for (DbRole role : cls.getLinks()) {
			role.set(dest, role.get(src));
		}
	}

	public Object update(Connection connection, DbClass entity, Object object) throws SQLException {
		Object oldObject = object;
		object = cache(entity, object);
		Object oldVersion = null;
		if (object != oldObject) {
			copy(entity, object, oldObject);
		}
		if (entity.getVersion() != null) {
			oldVersion = entity.getVersion().inc(object);
		}
		if (entity.getSuperClass() != null) {
			update(connection, entity.getSuperClass(), object);
		}
		Statements stmts = getUpdateStatement(connection, entity);
		PreparedStatement stmt = stmts.getUpdate();
		StringBuilder sql = null;
		if (logger != null) {
			sql = new StringBuilder(stmts.getUpdateSql());
		}
		int index = 1;
		for (DbField field : entity.getFields()) {
			if (!entity.getPrimaryKey().getFields().contains(field)) {
				Object value = field.get(object);
				if (value != null) {
					field.stmtSet(stmt, index++, value);
				} else {
					stmt.setNull(index++, getJDBCType(field.getType()).ordinal());
				}
				nextValue(sql, value);
			}
		}
		for (DbRole role : entity.getForeignKeys()) {
			Object child = role.get(object);
			DbClass cls = Database.getDbClass(role.getType());
			for (DbField field : cls.getPrimaryKey().getFields()) {
				Object value = child == null ? null : field.get(child);
				if (value != null) {
					stmt.setObject(index++, value);
				} else {
					stmt.setNull(index++, getJDBCType(field.getType()).ordinal());
				}
				nextValue(sql, value);
			}
		}
		for (DbField field : entity.getPrimaryKey().getFields()) {
			Object value = field.get(object);
			stmt.setObject(index++, value);
			nextValue(sql, value);
		}
		if (entity.getVersion() != null) {
			stmt.setObject(index++, oldVersion);
			nextValue(sql, oldVersion);
		}
		if (logger != null) {
			logger.accept(sql.toString());
		}
		if (stmt.executeUpdate() != 1) {
			throw new StaleObjectException(object + " is out of date.");
		}
		// update oversion & check on oversion in where clause
		return object;
	}

	public int setHierarchyValues(ResultSet rs, DbClass root, DbClass realClass, Object object, int index)
			throws SQLException {
		for (DbClass type : root.getTypeHierarchy()) {
			if (type.getType().isAssignableFrom(realClass.getType())) {
				index = setValues(rs, type, object, index);
			} else {
				index += type.getFields().size() - type.getPrimaryKey().getFields().size();
				for (DbRole role : type.getForeignKeys()) {
					index += role.getForeignKey().getFields().size();
				}
			}
		}
		return index;
	}

	public Object refresh(Connection connection, DbClass entity, Object object) throws SQLException {
		Statements stmts = getRefreshStatement(connection, entity);
		PreparedStatement stmt = stmts.getRefresh();
		StringBuilder sql = null;
		if (logger != null) {
			sql = new StringBuilder(stmts.getRefreshSql());
		}
		object = cache(entity, object);
		String currentSubtype = entity.getLabel();
		List<DbClass> types = entity.getTypeHierarchy();
		int index = 1;
		for (DbField field : entity.getPrimaryKey().getFields()) {
			Object value = field.get(object);
			field.stmtSet(stmt, index++, value);
			nextValue(sql, value);
		}
		if (logger != null) {
			logger.accept(sql.toString());
		}
		try (ResultSet rs = stmt.executeQuery()) {
			if (rs.next()) {
				index = 1;
				String subtype = null;
				if (entity.getRoot().hasSubTypeField()) {
					subtype = rs.getString(index++);
					DbClass realEntity = entity;
					if (!subtype.equals(currentSubtype)) {
						realEntity = entity.getSubClasses().get(subtype);
						Object newObject = realEntity.newInstance();
						for (DbField field : entity.getPrimaryKey().getFields()) {
							field.set(newObject, field.get(object));
						}
						object = newObject;
						replaceInCache(entity, object);
					}
					index = setHierarchyValues(rs, entity, realEntity, object, index);
				} else {
					for (DbClass c : types) {
						index = setValues(rs, c, object, index);
					}
				}
			}
		}
		return object;
	}

	protected int setPrimaryKey(ResultSet rs, DbClass cls, Object object, int index) throws SQLException {
		for (DbField field : cls.getPrimaryKey().getFields()) {
			Object value = field.rsGet(rs, index++);
			if (rs.wasNull()) {
				value = null;
			}
			field.set(object, value);
		}
		return index;
	}

	protected int setValues(ResultSet rs, DbClass cls, Object object, int index) throws SQLException {
		for (DbField field : cls.getFields()) {
			if (!cls.getPrimaryKey().getFields().contains(field)) {
				Object value = field.rsGet(rs, index++);
				if (rs.wasNull()) {
					value = null;
				}
				field.set(object, value);
			}
		}
		for (DbRole role : cls.getForeignKeys()) {
			if (!role.isPrimaryKeyRole()) {
				DbClass otherCls = Database.getDbClass(role.getType());
				Object other = null;
				for (DbField field : otherCls.getPrimaryKey().getFields()) {
					Object key = field.rsGet(rs, index++);
					if (rs.wasNull()) {
						break;
					} else if (other == null) {
						other = otherCls.newInstance();
						field.set(other, key);
					}
				}
				role.set(object, other);
			}
		}
		return index;
	}

	public String getDatabaseName(Connection connection) throws SQLException {
		return connection.getCatalog();
	}

	public void createIndex(Connection connection, DbSearchMethod path) throws SQLException {
		String fields = path.getFields().stream().map(f -> f.getName()).collect(Collectors.joining(","));
		createIndex(connection, path.getEntity().getName(), path.getName(), fields, path.isUnique());
	}

	public void createIndex(Connection connection, String tableName, String indexName, String fields, boolean unique)
			throws SQLException {
		StringBuilder sql = new StringBuilder("CREATE ");
		if (unique) {
			sql.append("UNIQUE ");
		}
		sql.append("INDEX IF NOT EXISTS ");
		sql.append(INDEX_PREFIX);
		sql.append(indexName);
		sql.append(" ON ");
		sql.append(tableName);
		sql.append("(");
		sql.append(fields);
		sql.append(")");
		execute(connection, sql.toString());
	}

	protected void createParentKey(Connection connection, DbClass cls) throws SQLException {
		StringBuilder sql = new StringBuilder("ALTER TABLE ");
		sql.append(cls.getName());
		sql.append(" ADD CONSTRAINT ");
		sql.append(FOREIGN_KEY_PREFIX);
		sql.append(cls.getName());
		sql.append("_parent");
		sql.append(" FOREIGN KEY(");
		sql.append(cls.getPrimaryKey().getFields().stream().map(a -> a.getName()).collect(Collectors.joining(",")));
		sql.append(") REFERENCES ");
		sql.append(cls.getSuperClass().getName());
		sql.append("(");
		sql.append(cls.getSuperClass().getPrimaryKey().getFields().stream().map(a -> a.getName())
				.collect(Collectors.joining(",")));
		sql.append(")");
		execute(connection, sql.toString());
	}

	public void upgradeParent(Connection connection, DbTable table, DbClass cls) throws SQLException {
		String name = cls.getName() + "_parent";
		if (table == null || table.getForeignKeys().get(FOREIGN_KEY_PREFIX + name) == null) {
			createParentKey(connection, cls);
		}
	}

	public void delete(Connection connection, DbClass entity, Object object) throws SQLException {
		Object oldObject = object;
		object = removeFromCache(entity, object);
		Object oldVersion = null;
		if (object != oldObject) {
			copy(entity, object, oldObject);
		}
		if (entity.getVersion() != null) {
			oldVersion = entity.getVersion().inc(object);
		}
		Statements stmts = getDeleteStatement(connection, entity);
		PreparedStatement stmt = stmts.getDelete();
		StringBuilder sql = null;
		if (logger != null) {
			sql = new StringBuilder(stmts.getDeleteSql());
		}
		int index = 1;
		for (DbField field : entity.getPrimaryKey().getFields()) {
			Object value = field.get(object);
			stmt.setObject(index++, value);
			nextValue(sql, value);
		}
		if (entity.getVersion() != null) {
			stmt.setObject(index++, oldVersion);
			nextValue(sql, oldVersion);
		}
		if (logger != null) {
			logger.accept(sql.toString());
		}
		if (stmt.executeUpdate() != 1) {
			throw new StaleObjectException(object + " is out of date.");
		}
		if (entity.getSuperClass() != null) {
			delete(connection, entity.getSuperClass(), object);
		}

	}

}
