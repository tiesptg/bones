package com.palisand.bones.persist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.palisand.bones.persist.CommandScheme.Metadata;
import com.palisand.bones.persist.CommandScheme.Metadata.DbIndex;
import com.palisand.bones.persist.CommandScheme.Metadata.DbTable;
import com.palisand.bones.persist.Database.DbClass.DbRole;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class Database {

	@FunctionalInterface
	public interface Transaction {
		void perform() throws SQLException;
	}

	@FunctionalInterface
	public interface TransactionWithResult {
		Object perform() throws SQLException;
	}

	@FunctionalInterface
	interface RsGetter {
		Object get(ResultSet rs, int pos) throws SQLException;
	}

	@FunctionalInterface
	interface StmtSetter {
		void set(PreparedStatement stmt, int pos, Object value) throws SQLException;
	}

	private static final Class<?>[] SUPPORTED_OBJECT_TYPES = { String.class, Boolean.class, Integer.class, Long.class,
			Double.class, Float.class, Short.class, BigDecimal.class, BigInteger.class, LocalDate.class, LocalDateTime.class,
			Calendar.class, Date.class, OffsetDateTime.class };
	static final Map<Class<?>, RsGetter> RS_GETTERS = new HashMap<>();
	static final Map<Class<?>, StmtSetter> STMT_SETTERS = new HashMap<>();
	private static final Map<Class<?>, Function<Object, Object>> INCREMENTERS = new HashMap<>();
	private static final Map<Connection, CommandScheme> COMMAND_SCHEMES = Collections
			.synchronizedMap(new WeakHashMap<>());

	static {
		RS_GETTERS.put(String.class, (rs, pos) -> rs.getString(pos));
		STMT_SETTERS.put(String.class, (rs, pos, value) -> rs.setString(pos, (String) value));
		RS_GETTERS.put(int.class, (rs, pos) -> rs.getInt(pos));
		STMT_SETTERS.put(int.class, (rs, pos, value) -> rs.setInt(pos, (Integer) value));
		RS_GETTERS.put(Integer.class, (rs, pos) -> rs.getInt(pos));
		STMT_SETTERS.put(Integer.class, (rs, pos, value) -> rs.setInt(pos, (Integer) value));
		RS_GETTERS.put(long.class, (rs, pos) -> rs.getLong(pos));
		STMT_SETTERS.put(long.class, (rs, pos, value) -> rs.setLong(pos, (Long) value));
		RS_GETTERS.put(Long.class, (rs, pos) -> rs.getLong(pos));
		STMT_SETTERS.put(Long.class, (rs, pos, value) -> rs.setLong(pos, (Long) value));
		RS_GETTERS.put(boolean.class, (rs, pos) -> rs.getBoolean(pos));
		STMT_SETTERS.put(boolean.class, (rs, pos, value) -> rs.setBoolean(pos, (Boolean) value));
		RS_GETTERS.put(Boolean.class, (rs, pos) -> rs.getBoolean(pos));
		STMT_SETTERS.put(Boolean.class, (rs, pos, value) -> rs.setBoolean(pos, (Boolean) value));
		RS_GETTERS.put(double.class, (rs, pos) -> rs.getDouble(pos));
		STMT_SETTERS.put(double.class, (rs, pos, value) -> rs.setDouble(pos, (Double) value));
		RS_GETTERS.put(Double.class, (rs, pos) -> rs.getDouble(pos));
		STMT_SETTERS.put(double.class, (rs, pos, value) -> rs.setDouble(pos, (Double) value));
		RS_GETTERS.put(LocalDate.class, (rs, pos) -> rs.getObject(pos, LocalDate.class));
		STMT_SETTERS.put(LocalDate.class, (rs, pos, value) -> rs.setObject(pos, (LocalDate) value));
		INCREMENTERS.put(int.class, i -> (Integer) i + 1);
		INCREMENTERS.put(Integer.class, i -> (Integer) i + 1);
		INCREMENTERS.put(long.class, i -> (Long) i + 1);
		INCREMENTERS.put(Long.class, i -> (Long) i + 1);
		INCREMENTERS.put(short.class, i -> (Short) i + 1);
		INCREMENTERS.put(Short.class, i -> (Short) i + 1);
		INCREMENTERS.put(byte.class, i -> (Byte) i + 1);
		INCREMENTERS.put(Byte.class, i -> (Byte) i + 1);
	}

	public static Class<?> getGenericType(Type type, int position) {
		try {
			ParameterizedType pType = (ParameterizedType) type;
			return (Class<?>) pType.getActualTypeArguments()[position];
		} catch (Exception ex) {
			// ignore
		}
		return null;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Id {
		boolean generated() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Version {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Relation {
		String opposite();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Repeatable(Indices.class)
	public @interface Index {
		String value();

		boolean unique() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Indices {
		Index[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Mapped {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface DontPersist {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Db {
		String name() default "";

		int size() default 0;

		int precision() default 0;

		boolean largeObject() default false;
	}

	@Setter
	@Getter
	public static class DbClass {
		private final Class<?> type;
		private final DbClass superClass;
		private final List<DbField> fields = new ArrayList<>();
		private final Map<String, DbClass> subClasses = new TreeMap<>();
		private DbSearchMethod primaryKey = null;
		private final Map<String, DbSearchMethod> indices = new TreeMap<>();
		private final List<DbRole> foreignKeys = new ArrayList<>();
		private final List<DbRole> links = new ArrayList<>();
		private final List<DbClass> typeHierarchy = new ArrayList<>();
		private final boolean mapped;
		private DbField version;
		private String label;

		@Setter
		@Getter
		@RequiredArgsConstructor
		public class DbSearchMethod {
			private String name;
			private List<DbField> fields = new ArrayList<>();
			private boolean unique = false;

			public DbClass getEntity() {
				return DbClass.this;
			}

			@Override
			public String toString() {
				return getName();
			}
		}

		@Setter
		@Getter
		@RequiredArgsConstructor
		public class DbRole {
			private Field field;
			private DbSearchMethod foreignKey;
			private Class<?> type;
			private DbRole opposite;
			private Method getter;
			private Method setter;
			private Id id = null;

			@Override
			public String toString() {
				return getName();
			}

			public String getName() {
				if (field == null) {
					return foreignKey.getName();
				}
				return field.getName();
			}

			public boolean isForeignKey() {
				// TODO: this excludes one-to-one associations at this time
				return !isMany();
			}

			public DbClass getEntity() {
				return DbClass.this;
			}

			public Relation getRelation() {
				return field.getAnnotation(Relation.class);
			}

			public boolean isMany() {
				return Collection.class.isAssignableFrom(field.getType());
			}

			public boolean isPrimaryKeyRole() {
				return id != null;
			}

			public Object get(Object owner) throws SQLException {
				try {
					return getter.invoke(owner);
				} catch (Exception ex) {
					if (ex.getCause() != null) {
						ex = (Exception) ex.getCause();
					}
					throw new SQLException("Could not get value of field " + field);
				}
			}

			@SuppressWarnings("unchecked")
			public void set(Object owner, Object value) throws SQLException {
				try {
					if (isMany()) {
						Collection<Object> col = (Collection<Object>) getter.invoke(owner);
						if (col == null) {
							if (field.getType() == List.class || field.getType() == Collection.class) {
								col = new ArrayList<>();
							} else if (field.getType() == Set.class) {
								col = new HashSet<>();
							} else {
								col = (Collection<Object>) field.getType().getConstructor().newInstance();
							}
							setter.invoke(owner, col);
						} else {
							col.clear();
						}
						if (value != null) {
							col.addAll((Collection<Object>) value);
						}
					} else {
						setter.invoke(owner, value);
					}
				} catch (Exception ex) {
					if (ex.getCause() != null) {
						ex = (Exception) ex.getCause();
					}
					throw new SQLException("Could not set value of field " + field, ex);
				}
			}

			public DbSearchMethod getForeignKey() throws SQLException {
				if (foreignKey == null) {
					DbClass entity = Database.getDbClass(type);
					foreignKey = new DbSearchMethod();
					foreignKey.setName(getEntity().getName() + '_' + getName());
					foreignKey.setUnique(false);
					entity.getPrimaryKey().getFields().forEach(field -> {
						DbForeignKeyAttribute copy = new DbForeignKeyAttribute();
						copy.setName(getName() + '_' + field.getName());
						copy.setType(field.getType());
						copy.setPrimaryKeyField(field);
						copy.setRole(this);
						foreignKey.getFields().add(copy);
					});
				}
				return foreignKey;
			}

			public DbRole getOpposite() throws SQLException {
				if (opposite == null) {
					Relation relation = getRelation();
					DbClass other = Database.getDbClass(type);
					if (other != null) {
						if (relation != null) {
							for (DbRole role : other.getForeignKeys()) {
								if (role.getName().equals(relation.opposite())) {
									opposite = role;
									break;
								}
							}
							for (DbRole role : other.getLinks()) {
								if (role.getName().equals(relation.opposite())) {
									opposite = role;
									break;
								}
							}
							if (opposite == null) {
								throw new SQLException(
										"Relation " + getName() + " points to not existing opposite " + relation.opposite());
							}
						} else {
							for (DbRole role : other.getForeignKeys()) {
								relation = role.getRelation();
								if (relation != null && relation.opposite().equals(getName())) {
									opposite = role;
									break;
								}
							}
							if (opposite == null) {
								for (DbRole role : other.getLinks()) {
									relation = role.getRelation();
									if (relation != null && relation.opposite().equals(getName())) {
										opposite = role;
										break;
									}
								}
							}
						}
					}
				}
				return opposite;
			}

			public String getTablename() throws SQLException {
				DbClass entity = Database.getDbClass(getType());
				return getName() + '_' + entity.getName();
			}

			public DbRole getFirst() throws SQLException {
				DbRole opposite = getOpposite();
				if (opposite != null) {
					if (getName().compareTo(opposite.getName()) > 0) {
						return opposite;
					}
				}
				return this;
			}

			public DbRole getSecond() throws SQLException {
				return getFirst().getOpposite();
			}

		}

		@Setter
		@Getter
		@RequiredArgsConstructor
		public class DbField {
			private Field field;
			private Method getter;
			private Method setter;
			private boolean nullable = true;
			private Id id;
			private Version version;
			private RsGetter rsGetter;
			private StmtSetter stmtSetter;
			private Function<Object, Object> incrementer = null;

			@Override
			public String toString() {
				return getName();
			}

			public String getName() {
				return field.getName();
			}

			public Class<?> getType() {
				return getter.getReturnType();
			}

			public DbClass getEntity() {
				return DbClass.this;
			}

			public boolean isGenerated() {
				return id == null ? false : id.generated();
			}

			public boolean isVersion() {
				return version != null;
			}

			public Object get(Object owner) throws SQLException {
				try {
					return getter.invoke(owner);
				} catch (Exception ex) {
					if (ex.getCause() != null) {
						ex = (Exception) ex.getCause();
					}
					throw new SQLException("Could not get value of field " + field);
				}
			}

			public Object inc(Object owner) throws SQLException {
				Object oldVersion = get(owner);
				set(owner, incrementer.apply(oldVersion));
				return oldVersion;
			}

			public void set(Object owner, Object value) throws SQLException {
				try {
					setter.invoke(owner, value);
				} catch (Exception ex) {
					if (ex.getCause() != null) {
						ex = (Exception) ex.getCause();
					}
					throw new SQLException("Could not set value of field " + field + " to " + value, ex);
				}
			}

			public Object rsGet(ResultSet rs, int pos) throws SQLException {
				return rsGetter.get(rs, pos);
			}

			public void stmtSet(PreparedStatement stmt, int pos, Object value) throws SQLException {
				stmtSetter.set(stmt, pos, value);
			}
		}

		@Getter
		@Setter
		@RequiredArgsConstructor
		public class DbForeignKeyAttribute extends DbField {
			private String name;
			private Class<?> type;
			private DbRole role;
			private DbField primaryKeyField;

			@Override
			public Object get(Object owner) throws SQLException {
				Object other = role.get(owner);
				if (other != null) {
					return primaryKeyField.get(other);
				}
				return null;
			}

			@Override
			public Object rsGet(ResultSet rs, int pos) throws SQLException {
				return primaryKeyField.rsGet(rs, pos);
			}

			public void set(Object owner, Object value) throws SQLException {
				if (value == null) {
					role.set(owner, null);
				} else {
					Object child = role.get(owner);
					if (child == null) {
						DbClass cls = Database.getDbClass(role.getType());
						child = cls.newInstance();
						role.set(owner, child);
					}
					primaryKeyField.set(child, value);
				}
			}

		}

		@Override
		public String toString() {
			return getName();
		}

		private DbClass registerSuperClass() throws SQLException {
			DbClass result = null;
			if (type.getSuperclass() != Object.class) {
				DbClass parent = Database.getDbClass(type.getSuperclass());
				if (parent.isMapped()) {
					fields.addAll(parent.getFields());
					indices.putAll(parent.getIndices());
					foreignKeys.addAll(parent.getForeignKeys());
					version = parent.getVersion();
					DbClass indirectParent = parent.getSuperClass();
					while (indirectParent != null && indirectParent.isMapped()) {
						indirectParent = indirectParent.getSuperClass();
					}
					result = indirectParent;
				} else {
					fields.addAll(parent.getPrimaryKey().getFields());
					result = parent;
				}
			} else {
				result = null;
			}
			return result;
		}

		private DbRole newRole(Field field, boolean collection) {
			DbRole role = new DbRole();
			if (collection) {
				role.setType(getGenericType(field.getGenericType(), 0));
			} else {
				role.setType(field.getType());
			}
			role.setField(field);
			role.setGetter(getGetter(type, field));
			role.setSetter(getSetter(type, field));
			return role;
		}

		private DbField newAttribute(Field field) {
			DbField attribute = new DbField();
			attribute.setField(field);
			attribute.setGetter(getGetter(type, field));
			attribute.setSetter(getSetter(type, field));
			attribute.setRsGetter(RS_GETTERS.get(field.getType()));
			attribute.setStmtSetter(STMT_SETTERS.get(field.getType()));
			attribute.setNullable(!field.getType().isPrimitive());
			fields.add(attribute);
			Index[] all = field.getAnnotationsByType(Index.class);
			for (Index index : all) {
				DbSearchMethod path = indices.get(index.value());
				if (path == null) {
					path = new DbSearchMethod();
					path.setName(getName() + '_' + index.value());
					indices.put(index.value(), path);
				}
				path.fields.add(attribute);
			}
			return attribute;
		}

		private DbField initVersion(DbField field) {
			Version version = field.getField().getAnnotation(Version.class);
			if (version != null) {
				field.setIncrementer(INCREMENTERS.get(field.getType()));
				field.setVersion(version);
				return field;
			}
			return null;
		}

		private String getLabel(String name) {
			if (name.length() > 3) {
				return name.substring(0, 3);
			}
			return name;
		}

		private String nextLabel(String label) throws SQLException {
			String base = label;
			int nr = 0;
			if (label.length() == 4) {
				base = label.substring(0, 3);
				nr = Integer.valueOf(label.substring(3));
				if (nr > 8) {
					throw new SQLException("too many classes start with " + base);
				}
			}
			return base + (++nr);
		}

		private String addSubclass(String label, DbClass subClass) throws SQLException {
			if (superClass != null) {
				label = superClass.addSubclass(label, subClass);
				subClasses.put(label, subClass);
			} else {
				if (this.label == null) {
					this.label = getLabel(getName());
					subClasses.put(this.label, this);
				}
				while (subClasses.containsKey(label)) {
					label = nextLabel(label);
				}
				subClasses.put(label, subClass);
				subClass.setLabel(label);
			}
			return label;
		}

		private void registerSubclasses() throws SQLException {
			if (superClass != null) {
				this.label = superClass.addSubclass(getLabel(getName()), this);
				Map<String, DbClass> changed = new TreeMap<>();
				Set<String> removedKeys = new TreeSet<>();
				for (Entry<String, DbClass> entry : subClasses.entrySet()) {
					String label = superClass.addSubclass(entry.getKey(), entry.getValue());
					if (!label.equals(entry.getKey())) {
						changed.put(label, entry.getValue());
						removedKeys.add(entry.getKey());
					}
				}
				removedKeys.forEach(key -> subClasses.remove(key));
				changed.forEach((key, value) -> {
					subClasses.put(key, value);
					value.setLabel(key);
				});
			}
		}

		public boolean hasSubTypeField() {
			return superClass == null && !subClasses.isEmpty();
		}

		public DbField getField(String name) {
			for (DbField field : fields) {
				if (field.getName().equalsIgnoreCase(name)) {
					return field;
				}
			}
			return null;
		}

		public DbRole getForeignKey(String name) {
			for (DbRole role : foreignKeys) {
				if (role.getName().equalsIgnoreCase(name)) {
					return role;
				}
			}
			return null;
		}

		public List<DbField> getFields() throws SQLException {
			// this is necessary becaus getPrimaryKeys can change the order of fields
			getPrimaryKey();
			return fields;
		}

		public DbSearchMethod getPrimaryKey() throws SQLException {
			if (primaryKey == null) {
				primaryKey = new DbSearchMethod();
				primaryKey.setName("pk");
				primaryKey.setUnique(true);
				for (DbField field : getFields()) {
					Id id = field.getField().getAnnotation(Id.class);
					if (id != null) {
						primaryKey.fields.add(field);
						field.setId(id);
					}
				}
				for (int i = 0; i < primaryKey.getFields().size(); ++i) {
					DbField field = primaryKey.getFields().get(i);
					getFields().remove(field);
					getFields().add(i, field);
				}
				for (DbRole role : getForeignKeys()) {
					Id id = role.getField().getAnnotation(Id.class);
					if (id != null) {
						role.setId(id);
						DbSearchMethod fk = role.getForeignKey();
						for (DbField field : fk.getFields()) {
							primaryKey.getFields().add(field);
						}
					}
				}
			}
			return primaryKey;
		}

		public DbRole getLink(String name) {
			for (DbRole role : links) {
				if (role.getName().equalsIgnoreCase(name)) {
					return role;
				}
			}
			return null;
		}

		public DbClass(Class<?> cls) throws SQLException {
			type = cls;
			superClass = registerSuperClass();
			mapped = cls.getAnnotation(Mapped.class) != null;
			for (Field field : cls.getDeclaredFields()) {
				if (field.getAnnotation(DontPersist.class) == null) {
					if (Collection.class.isAssignableFrom(field.getType())) {
						links.add(newRole(field, true));
					} else if (!field.getType().isPrimitive() && !field.getType().getName().startsWith("java")) {
						foreignKeys.add(newRole(field, false));
					} else if (field.getType().isPrimitive() || isSupported(field.getType())) {
						DbField attribute = newAttribute(field);
						DbField withVersion = initVersion(attribute);
						if (withVersion != null) {
							version = withVersion;
						}
					} else {
						throw new SQLException("Field " + field.getName() + " has unsupported type " + field.getType());
					}
				}
			}
			if (!isMapped()) {
				registerSubclasses();
			}
			ENTITIES.put(cls, this);
		}

		private static void addSubclass(List<DbClass> list, DbClass cls) {
			if (!list.contains(cls)) {
				DbClass superClass = cls.getSuperClass();
				if (superClass != null) {
					addSubclass(list, cls.getSuperClass());
				}
				list.add(cls);
			}
		}

		public List<DbClass> getTypeHierarchy() {
			if (typeHierarchy.isEmpty()) {
				DbClass c = getSuperClass();
				while (c != null) {
					typeHierarchy.add(0, c);
					c = c.getSuperClass();
				}
				typeHierarchy.add(this);
				for (DbClass child : getSubClasses().values()) {
					addSubclass(typeHierarchy, child);
				}
			}
			return typeHierarchy;
		}

		public String getName() {
			return type.getSimpleName();
		}

		private boolean isSupported(Class<?> cls) {
			return Arrays.stream(SUPPORTED_OBJECT_TYPES).anyMatch(c -> c == cls);
		}

		public DbClass getRoot() {
			if (superClass != null) {
				return superClass.getRoot();
			}
			return this;
		}

		public String getPrimaryKeyAsString(Object object) throws SQLException {
			StringBuilder sb = new StringBuilder();
			for (DbField field : getPrimaryKey().getFields()) {
				Object value = field.get(object);
				if (value != null) {
					sb.append(value).append('|');
				}
			}
			return sb.toString();
		}

		private Method getGetter(Class<?> cls, Field field) {
			try {
				String name = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
				if (field.getType() == Boolean.class || field.getType() == boolean.class) {
					return cls.getMethod("is" + name);
				}
				return cls.getMethod("get" + name);
			} catch (NoSuchMethodException | SecurityException ex) {
				return null;
			}
		}

		private Method getSetter(Class<?> cls, Field field) {
			try {
				String name = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
				return cls.getMethod("set" + name, field.getType());
			} catch (NoSuchMethodException | SecurityException ex) {
				return null;
			}
		}

		public Object newInstance() throws SQLException {
			try {
				return getType().getConstructor().newInstance();
			} catch (Exception ex) {
				if (ex.getCause() != null) {
					throw new SQLException(ex.getCause());
				}
				throw new SQLException(ex);
			}
		}

	}

	private static final Map<Class<?>, DbClass> ENTITIES = new ConcurrentHashMap<>();
	private final Supplier<CommandScheme> commandsCreator;

	public Database() {
		this(() -> new CommandScheme());
	}

	public Database(Supplier<CommandScheme> commandSupplier) {
		commandsCreator = commandSupplier;
	}

	public void setLogger(Connection connection, Consumer<String> logger) {
	}

	private CommandScheme getCommands(Connection connection) throws SQLException {
		CommandScheme result = COMMAND_SCHEMES.get(connection);
		if (result == null) {
			result = commandsCreator.get();
			COMMAND_SCHEMES.put(connection, result);
			connection.setAutoCommit(false);
		}
		return (CommandScheme) result;
	}

	synchronized static DbClass getDbClass(Class<?> cls) throws SQLException {
		DbClass entity = ENTITIES.get(cls);
		if (entity == null) {
			entity = new DbClass(cls);
			ENTITIES.put(cls, entity);
		}
		return entity;
	}

	public void commit(Connection connection) throws SQLException {
		connection.commit();
		CommandScheme commands = getCommands(connection);
		commands.clearCache();
	}

	public void rollback(Connection connection) throws SQLException {
		connection.rollback();
	}

	public void register(Class<?>... persistentClasses) throws SQLException {
		for (Class<?> c : persistentClasses) {
			Database.getDbClass(c);
		}
	}

	public boolean verify(Connection connection, Class<?>... types) throws SQLException {
		String error = "Database.verify error";
		CommandScheme commands = getCommands(connection);
		Consumer<String> logger = commands.getLogger();
		try {
			commands.logger(str -> {
				if (logger != null)
					logger.accept("Verification Error: " + str);
				throw new RuntimeException(error);
			});
			upgrade(connection, types);
			return true;
		} catch (RuntimeException ex) {
			if (ex.getMessage().equals(error)) {
				return false;
			}
			throw ex;
		} finally {
			commands.logger(logger);
		}
	}

	public void upgrade(Connection connection, Class<?>... types) throws SQLException {
		register(types);
		CommandScheme commands = getCommands(connection);
		HashSet<DbRole> fks = new HashSet<>();
		List<DbClass> withParents = new ArrayList<>();
		Set<String> tableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (Class<?> type : types) {
			DbClass dbc = Database.getDbClass(type);
			if (!dbc.isMapped()) {
				tableNames.add(dbc.getName());
			}
		}
		Metadata metadata = commands.getMetadata(connection);
		TreeSet<String> tablesRemoved = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		tablesRemoved.addAll(metadata.getTables().keySet());
		for (DbTable table : metadata.getTables().values()) {
			for (DbIndex fk : table.getForeignKeys().values()) {
				if (!tableNames.contains(fk.getReferences().getName())) {
					commands.dropContraint(connection, table.getName(), fk.getName());
				}
			}
		}
		for (Class<?> type : types) {
			DbClass entity = getDbClass(type);
			if (!entity.isMapped()) {
				if (entity.getSuperClass() != null) {
					withParents.add(entity);
				}
				commands.upgradeTable(connection, metadata.getTables().get(entity.getName()), entity);
				tablesRemoved.remove(entity.getName());
				fks.addAll(entity.getForeignKeys());
			}
		}
		for (DbClass cls : withParents) {
			DbTable table = metadata.getTables().get(cls.getName());
			commands.upgradeParent(connection, table, cls);
		}
		for (DbRole role : fks) {
			DbTable table = metadata.getTables().get(role.getEntity().getName());
			commands.upgradeForeignKey(connection, table, role);
		}
		for (String name : tablesRemoved) {
			commands.dropTable(connection, name);
		}

	}

	public void dropAll(Connection connection) throws SQLException {
		CommandScheme commands = getCommands(connection);
		Metadata md = commands.getMetadata(connection);
		for (DbTable table : md.getTables().values()) {
			for (DbIndex fk : table.getForeignKeys().values()) {
				commands.dropContraint(connection, table.getName(), fk.getName());
			}
		}
		for (DbTable table : md.getTables().values()) {
			commands.dropTable(connection, table.getName());
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T insert(Connection connection, T object) throws SQLException {
		CommandScheme commands = getCommands(connection);
		DbClass entity = getDbClass(object.getClass());
		return (T) commands.insert(connection, entity, entity.getLabel(), object);
	}

	@SuppressWarnings("unchecked")
	public <T> T update(Connection connection, T object) throws SQLException {
		CommandScheme commands = getCommands(connection);
		DbClass entity = getDbClass(object.getClass());
		return (T) commands.update(connection, entity, object);
	}

	public void delete(Connection connection, Object object) throws SQLException {
		CommandScheme commands = getCommands(connection);
		DbClass entity = getDbClass(object.getClass());
		commands.delete(connection, entity, object);
	}

	@SuppressWarnings("unchecked")
	public <T> T refresh(Connection connection, T object) throws SQLException {
		CommandScheme commands = getCommands(connection);
		DbClass cls = Database.getDbClass(object.getClass());
		return (T) commands.refresh(connection, cls, object);
	}

	public String getDatabaseName(Connection connection) throws SQLException {
		CommandScheme commands = getCommands(connection);
		return commands.getDatabaseName(connection);
	}

	public <X> Query<X> newQuery(Connection connection, Class<X> queryType) throws SQLException {
		return newQuery(connection, queryType, null);
	}

	public <X> Query<X> newQuery(Connection connection, Class<X> queryType, String alias) throws SQLException {
		CommandScheme commands = getCommands(connection);
		Query<X> query = new Query<X>(connection, commands, queryType);
		if (!queryType.isRecord() && !queryType.getPackage().getName().startsWith("java")) {
			return query.selectFrom(queryType, alias);
		}
		return query;
	}

	public Object transactionWithResult(Connection connection, TransactionWithResult transaction) throws SQLException {
		try {
			Object result = transaction.perform();
			commit(connection);
			return result;
		} catch (SQLException ex) {
			rollback(connection);
			throw ex;
		} catch (Exception ex) {
			rollback(connection);
			throw new SQLException(ex);
		}
	}

	public void transaction(Connection connection, Transaction transaction) throws SQLException {
		try {
			transaction.perform();
			commit(connection);
		} catch (SQLException ex) {
			rollback(connection);
			throw ex;
		} catch (Exception ex) {
			rollback(connection);
			throw new SQLException(ex);
		}
	}

}
