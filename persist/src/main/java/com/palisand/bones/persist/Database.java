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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.palisand.bones.persist.CommandScheme.Metadata;
import com.palisand.bones.persist.CommandScheme.Metadata.DbIndex;
import com.palisand.bones.persist.CommandScheme.Metadata.DbTable;
import com.palisand.bones.persist.Database.DbClass.DbRole;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class Database {
  private static final Class<?>[] SUPPORTED_OBJECT_TYPES = 
    { String.class, Boolean.class, Integer.class, Long.class, Double.class, 
        Float.class, Short.class, BigDecimal.class, BigInteger.class, 
        LocalDate.class, LocalDateTime.class, Calendar.class, Date.class, 
        OffsetDateTime.class };
  
  public static Class<?> getGenericType(Type type, int position) {
    try {
      ParameterizedType pType = (ParameterizedType)type;
          return(Class<?>)pType.getActualTypeArguments()[position];
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
  
  @Setter
  @Getter
  public static class DbClass {
    private final Class<?> type;
    private final DbClass superClass;
    private final List<DbField> fields = new ArrayList<>();
    private final DbSearchMethod primaryKey = new DbSearchMethod();
    private final Map<String,DbSearchMethod> indices = new TreeMap<>();
    private final List<DbRole> foreignKeys = new ArrayList<>();
    private final List<DbRole> links = new ArrayList<>();
    private final boolean mapped;
    private final DbField version;

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
      
      public String getName() {
        if (field == null) {
          return foreignKey.getName();
        }
        return field.getName();
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
      
      public Object get(Object owner) throws SQLException {
        try {
          return getter.invoke(owner);
        } catch (Exception ex) {
          if (ex.getCause() != null) {
            ex = (Exception)ex.getCause();
          }
          throw new SQLException("Could not get value of field " + field);
        }
      }
      
      public void set(Object owner,Object value) throws SQLException {
        try {
          setter.invoke(owner,value);
        } catch (Exception ex) {
          if (ex.getCause() != null) {
            ex = (Exception)ex.getCause();
          }
          throw new SQLException("Could not set value of field " + field);
        }
      }

      public DbSearchMethod getForeignKey() throws SQLException {
        if (foreignKey == null && !isMany()) {
          DbClass entity = Database.getDbClass(type);
          foreignKey = new DbSearchMethod();
          foreignKey.setName(CommandScheme.FOREIGN_KEY_PREFIX + getEntity().getName() + '_' + getName());
          foreignKey.setUnique(false);
          entity.getPrimaryKey().getFields().forEach(field -> {
            DbForeignKeyAttribute copy = new DbForeignKeyAttribute();
            copy.setName(getName() + '_' + field.getName());
            copy.setType(field.getType());
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
              for (DbRole role: other.getForeignKeys()) {
                if (role.getName().equals(relation.opposite())) {
                  opposite = role;
                  break;
                }
              }
              for (DbRole role: other.getLinks()) {
                if (role.getName().equals(relation.opposite())) {
                  opposite = role;
                  break;
                }
              }
            } else {
              for (DbRole role: other.getForeignKeys()) {
                relation = role.getRelation();
                if (relation != null && relation.opposite().equals(getName())) {
                  opposite = role;
                  break;
                }
              }
              for (DbRole role: other.getLinks()) {
                if (role.getName().equals(relation.opposite())) {
                  opposite = role;
                  break;
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
            ex = (Exception)ex.getCause();
          }
          throw new SQLException("Could not get value of field " + field);
        }
      }
      
      public void set(Object owner,Object value) throws SQLException {
        try {
          setter.invoke(owner,value);
        } catch (Exception ex) {
          if (ex.getCause() != null) {
            ex = (Exception)ex.getCause();
          }
          throw new SQLException("Could not get value of field " + field);
        }
      }
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public class DbForeignKeyAttribute extends DbField {
      private String name;
      private Class<?> type;
    }
  
    public DbClass(Class<?> cls) throws SQLException {
      primaryKey.setUnique(true);
      type = cls;
      if (cls.getSuperclass() != Object.class) {
        DbClass parent = Database.getDbClass(cls.getSuperclass());
        if (parent.isMapped()) {
          fields.addAll(parent.getFields());
          indices.putAll(parent.getIndices());
          foreignKeys.addAll(parent.getForeignKeys());
          DbClass indirectParent = parent.getSuperClass();
          while (indirectParent != null && indirectParent.isMapped()) {
            indirectParent = indirectParent.getSuperClass();
          }
          superClass = indirectParent;
        } else {
          fields.addAll(parent.getPrimaryKey().getFields());
          superClass = parent;
        }
        primaryKey.getFields().addAll(parent.getPrimaryKey().getFields());
      } else {
        superClass = null;
      }
      mapped = cls.getAnnotation(Mapped.class) != null;
      DbField dbVersion = null;
      for (Field field: cls.getDeclaredFields()) {
        if (field.getAnnotation(DontPersist.class) == null) {
          if (Collection.class.isAssignableFrom(field.getType())) {
            DbRole role = new DbRole();
            role.setType(getGenericType(field.getGenericType(),0));
            role.setField(field);
            role.setGetter(getGetter(cls,field));
            role.setSetter(getSetter(cls,field));
            links.add(role);
          } else if (!field.getType().isPrimitive() && !field.getType().getName().startsWith("java")) {
            DbRole role = new DbRole();
            role.setType(field.getType());
            role.setField(field);
            role.setGetter(getGetter(cls,field));
            role.setSetter(getSetter(cls,field));
            foreignKeys.add(role);
          } else if (field.getType().isPrimitive() || isSupported(field.getType())) {
            DbField attribute = new DbField();
            attribute.setField(field);
            attribute.setGetter(getGetter(cls,field));
            attribute.setSetter(getSetter(cls,field));
            attribute.setNullable(!field.getType().isPrimitive());
            fields.add(attribute);
            Id id = field.getAnnotation(Id.class);
            if (id != null) {
              primaryKey.fields.add(attribute);
              attribute.setId(id);
            }
            Version version = field.getAnnotation(Version.class);
            if (version != null) {
            	attribute.setVersion(version);
            	dbVersion = attribute;
            } else {
            	dbVersion = null;
            }
            Index[] all = field.getAnnotationsByType(Index.class);
            for (Index index: all) {
              DbSearchMethod path = indices.get(index.value());
              if (path == null) {
                path = new DbSearchMethod();
                path.setName(getName() + '_' + index.value());
                indices.put(index.value(),path);
              }
              path.fields.add(attribute);
            }
          } else {
            throw new SQLException("Field " + field.getName() + " has unsupported type " + field.getType());
          }
        }
      }
      this.version = dbVersion;
      ENTITIES.put(cls,this);
    }
    
    public String getName() {
      return type.getSimpleName();
    }
    
    private boolean isSupported(Class<?> cls) {
      return Arrays.stream(SUPPORTED_OBJECT_TYPES).anyMatch(c -> c == cls);
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
  
  private static final Map<Class<?>,DbClass> ENTITIES = new ConcurrentHashMap<>();
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
    Map<String,Class<?>> result = connection.getTypeMap();
    if (result == null || !(result instanceof CommandScheme)) {
      result = commandsCreator.get();
      connection.setTypeMap(result);
      connection.setAutoCommit(false);
    }
    return (CommandScheme)result;
  }
  
  synchronized static DbClass getDbClass(Class<?> cls) throws SQLException {
    DbClass entity = ENTITIES.get(cls);
    if (entity == null) {
      entity = new DbClass(cls);
      ENTITIES.put(cls,entity);
    }
    return entity;
  }
  
  public void startTransaction(Connection connection) throws SQLException {
  }
  
  public void commit(Connection connection) throws SQLException {
    connection.commit();
  }
  
  public void rollback(Connection connection) throws SQLException {
    connection.rollback();
  }
  
  public void upgrade(Connection connection, Class<?>...types) throws SQLException {
    CommandScheme commands = getCommands(connection);
    HashSet<DbRole> m2m = new HashSet<>();
    HashSet<DbRole> fks = new HashSet<>();
    List<DbClass> withParents = new ArrayList<>();
    Set<String> tableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (Class<?> type: types) {
      DbClass dbc = Database.getDbClass(type);
      tableNames.add(dbc.getName());
    }
    Metadata metadata = commands.getMetadata(connection);
    TreeSet<String> tablesRemoved = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    tablesRemoved.addAll(metadata.getTables().keySet());
    for (DbTable table: metadata.getTables().values()) {
      for (DbIndex fk: table.getForeignKeys().values()) {
        if (!tableNames.contains(fk.getReferences().getName())) {
          commands.dropContraint(connection,table.getName(),fk.getName());
        }
      }
    }
    for (Class<?> type: types) {
      DbClass entity = getDbClass(type);
      if (entity.getSuperClass() != null) {
        withParents.add(entity);
      }
      commands.upgradeTable(connection, metadata.getTables().get(entity.getName()), entity);
      tablesRemoved.remove(entity.getName());
      for (DbRole role: entity.getLinks()) {
        if (role.getOpposite() != null && role.getOpposite().isMany()) {
          m2m.add(role.getFirst());
        }
      }
      fks.addAll(entity.getForeignKeys());
    }
    for (DbClass cls: withParents) {
      DbTable table = metadata.getTables().get(cls.getName());
      commands.upgradeParent(connection, table, cls);
    }
    for (DbRole role: m2m) {
      Metadata.DbTable table = metadata.getTables().get(role.getTablename());
      String tableName = role.getTablename();
      if (tableName != null) {
        if (!tablesRemoved.remove(tableName)) {
          commands.createLinkTable(connection,table,role);
        }
      }
    }
    for (DbRole role: fks) {
      DbTable table = metadata.getTables().get(role.getEntity().getName());
      commands.upgradeForeignKey(connection, table, role);
    }
    for (String name: tablesRemoved) {
      commands.dropTable(connection,name);
    }
    
  }
  
  public void dropAll(Connection connection) throws SQLException {
    CommandScheme commands = getCommands(connection);
    Metadata md = commands.getMetadata(connection);
    for (DbTable table: md.getTables().values()) {
      for (DbIndex fk: table.getForeignKeys().values()) {
        commands.dropContraint(connection,table.getName(),fk.getName());
      }
    }
    for (DbTable table: md.getTables().values()) {
      commands.dropTable(connection,table.getName());
    }
  }
  
  public void insert(Connection connection, Object...objects) throws SQLException {
    CommandScheme commands = getCommands(connection);
    for (Object object: objects) {
      DbClass entity = getDbClass(object.getClass());
      commands.insert(connection, entity, object);
    }
  }
  
  public void update(Connection connection, Object...objects) throws SQLException {
    CommandScheme commands = getCommands(connection);
    for (Object object: objects) {
      DbClass entity = getDbClass(object.getClass());
      commands.update(connection, entity, object);
    }
  }
  
  public String getDatabaseName(Connection connection) throws SQLException {
    CommandScheme commands = getCommands(connection);
    return commands.getDatabaseName(connection);
  }

}
