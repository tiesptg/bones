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
import java.util.function.Supplier;

import com.palisand.bones.persist.CommandScheme.Metadata;
import com.palisand.bones.persist.Database.Entity.Role;

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
  public static class Entity {
    private final Class<?> type;
    private final List<Attribute> fields = new ArrayList<>();
    private final SearchPath primaryKey = new SearchPath();
    private final Map<String,SearchPath> indices = new TreeMap<>();
    private final List<Role> foreignKeys = new ArrayList<>();
    private final List<Role> links = new ArrayList<>();
    private final boolean mapped;

    @Setter
    @Getter
    @RequiredArgsConstructor
    public class SearchPath {
      private String name;
      private List<Attribute> fields = new ArrayList<>();
      private boolean unique = false;

      public Entity getEntity() {
        return Entity.this;
      }
    }
    
    @Setter
    @Getter
    @RequiredArgsConstructor
    public class Role {
      private Field field;
      private SearchPath foreignKey;
      private Class<?> type;
      private Role opposite;
      private Method getter;
      private Method setter;
      
      public String getName() {
        return field.getName();
      }
      
      public Entity getEntity() {
        return Entity.this;
      }

      public Relation getRelation() {
        return field.getAnnotation(Relation.class);
      }
      
      public boolean isMany() {
        return Collection.class.isAssignableFrom(field.getType());
      }
      
      public SearchPath getForeignKey() throws SQLException {
        if (foreignKey == null && !isMany()) {
          Entity entity = Database.getEntity(type);
          foreignKey = new SearchPath();
          foreignKey.setName(CommandScheme.FOREIGN_KEY_PREFIX + field.getName());
          foreignKey.setUnique(false);
          entity.getPrimaryKey().getFields().forEach(field -> {
            ForeignKeyAttribute copy = new ForeignKeyAttribute();
            copy.setName(getName() + field.getName());
            copy.setType(field.getType());
            foreignKey.getFields().add(copy); 
          });
        }
        return foreignKey;
      }
      
      public Role getOpposite() throws SQLException {
        if (opposite == null) {
          Relation relation = getRelation();
          Entity other = Database.getEntity(type);
          if (other != null) {
            if (relation != null) {
              for (Role role: other.getForeignKeys()) {
                if (role.getName().equals(relation.opposite())) {
                  opposite = role;
                  break;
                }
              }
              for (Role role: other.getLinks()) {
                if (role.getName().equals(relation.opposite())) {
                  opposite = role;
                  break;
                }
              }
            } else {
              for (Role role: other.getForeignKeys()) {
                relation = role.getRelation();
                if (relation != null && relation.opposite().equals(getName())) {
                  opposite = role;
                  break;
                }
              }
              for (Role role: other.getLinks()) {
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
        Entity entity = Database.getEntity(getType());
        return getName() + entity.getName();
      }

      public Role getFirst() throws SQLException {
        Role opposite = getOpposite();
        if (opposite != null) {
          if (getName().compareTo(opposite.getName()) > 0) {
            return opposite;
          }
        }
        return this;
      }
      
      public Role getSecond() throws SQLException {
        return getFirst().getOpposite();
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

      
    }
    
    @Setter
    @Getter
    @RequiredArgsConstructor
    public class Attribute {
      private Field field;
      private Method getter;
      private Method setter;
      private boolean nullable = true;
      private Id id;

      public String getName() {
        return field.getName();
      }
      
      public Class<?> getType() {
        return getter.getReturnType();
      }
      
      public Entity getEntity() {
        return Entity.this;
      }
      
      public boolean isGenerated() {
        return id == null ? false : id.generated();
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
    public class ForeignKeyAttribute extends Attribute {
      private String name;
      private Class<?> type;
    }
    

  
    public Entity(Class<?> cls) throws SQLException {
      primaryKey.setUnique(true);
      type = cls;
      if (cls.getSuperclass() != Object.class) {
        Entity parent = Database.getEntity(cls.getSuperclass());
        if (parent.isMapped()) {
          fields.addAll(parent.getFields());
          indices.putAll(parent.getIndices());
          foreignKeys.addAll(parent.getForeignKeys());
        } else {
          fields.addAll(parent.getPrimaryKey().getFields());
        }
        primaryKey.getFields().addAll(parent.getPrimaryKey().getFields());
      }
      mapped = cls.getAnnotation(Mapped.class) != null;
      for (Field field: cls.getDeclaredFields()) {
        if (field.getAnnotation(DontPersist.class) == null) {
          if (Collection.class.isAssignableFrom(field.getType())) {
            Role role = new Role();
            role.setType(getGenericType(field.getGenericType(),0));
            role.setField(field);
            role.setGetter(getGetter(cls,field));
            role.setSetter(getSetter(cls,field));
            links.add(role);
          } else if (!field.getType().isPrimitive() && !field.getType().getName().startsWith("java")) {
            Role role = new Role();
            role.setType(field.getType());
            role.setField(field);
            role.setGetter(getGetter(cls,field));
            role.setSetter(getSetter(cls,field));
            foreignKeys.add(role);
          } else if (field.getType().isPrimitive() || isSupported(field.getType())) {
            Attribute attribute = new Attribute();
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
            Index[] all = field.getAnnotationsByType(Index.class);
            for (Index index: all) {
              SearchPath path = indices.get(index.value());
              if (path == null) {
                path = new SearchPath();
                path.setName(index.value());
                indices.put(index.value(),path);
              }
              path.fields.add(attribute);
            }
          } else {
            throw new SQLException("Field " + field.getName() + " has unsupported type " + field.getType());
          }
        }
      }
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
    
  }
  
  private static final Map<Class<?>,Entity> ENTITIES = new ConcurrentHashMap<>();
  private final Supplier<CommandScheme> commandsCreator;
  
  public Database() {
    this(() -> new CommandScheme());
  }
  
  public Database(Supplier<CommandScheme> commandSupplier) {
    commandsCreator = commandSupplier;
  }
  
  private CommandScheme getCommands(Connection connection) throws SQLException {
    Map<String,Class<?>> result = connection.getTypeMap();
    if (result == null || !(result instanceof CommandScheme)) {
      result = commandsCreator.get();
      connection.setTypeMap(result);
    }
    return (CommandScheme)result;
  }
  
  synchronized static Entity getEntity(Class<?> cls) throws SQLException {
    Entity entity = ENTITIES.get(cls);
    if (entity == null) {
      entity = new Entity(cls);
      ENTITIES.put(cls,entity);
    }
    return entity;
  }
  
  private List<Entity> entitiesInTransaction = null;
  
  public void startTransaction(Connection connection) throws SQLException {
    if (entitiesInTransaction != null) {
      throw new SQLException("Already in transaction");
    }
    entitiesInTransaction = new ArrayList<>();
  }
  
  public void commit(Connection connection) throws SQLException {
    entitiesInTransaction = null;
    connection.commit();
  }
  
  public void rollback(Connection connection) throws SQLException {
    entitiesInTransaction = null;
    connection.rollback();
  }
  
  public void upgrade(Connection connection, Class<?>...types) throws SQLException {
    CommandScheme commands = getCommands(connection);
    HashSet<Role> m2m = new HashSet<>();
    HashSet<Role> fks = new HashSet<>();
    Metadata metadata = commands.getMetadata(connection);
    TreeSet<String> tablesRemoved = new TreeSet<>(metadata.getTables().keySet());
    for (Class<?> type: types) {
      Entity entity = getEntity(type);
      commands.upgradeTable(connection, metadata.getTables().get(entity.getName().toLowerCase()), entity);
      tablesRemoved.remove(entity.getName());
      for (Role role: entity.getLinks()) {
        if (role.getOpposite() != null && role.getOpposite().isMany()) {
          m2m.add(role.getFirst());
        }
      }
      fks.addAll(entity.getForeignKeys());
    }
    for (Role role: m2m) {
      Metadata.Table table = metadata.getTables().get(role.getTablename().toLowerCase());
      String tableName = commands.createLinkTable(connection, table, role);
      if (tableName != null) {
        tablesRemoved.remove(tableName);
      }
    }
    for (Role role: fks) {
      commands.createForeignKey(connection, role);
      commands.createIndex(connection,role.getForeignKey());
    }
    for (String name: tablesRemoved) {
      commands.dropTable(connection,name);
    }
    
  }
  
  public void drop(Connection connection, Class<?>...types) throws SQLException {
    CommandScheme commands = getCommands(connection);
    List<Entity> entities = new ArrayList<>();
    for (Class<?> type: types) {
      Entity entity = getEntity(type);
      entities.add(entity);
      for (Role role: entity.getForeignKeys()) {
        commands.dropContraint(connection, entity,role);
      }
      for (Role role: entity.getLinks()) {
        if (role.getOpposite() != null && role.getOpposite().isMany()) {
          commands.dropTable(connection, role);
        }
      }
    }
    for (Entity entity: entities) {
      commands.dropTable(connection,entity.getName());
    }
  }
  
  public void insert(Connection connection, Object...objects) throws SQLException {
    CommandScheme commands = getCommands(connection);
    for (Object object: objects) {
      Entity entity = getEntity(object.getClass());
      commands.insert(connection, entity, object);
    }
  }
  
  public String getDatabaseName(Connection connection) throws SQLException {
    CommandScheme commands = getCommands(connection);
    return commands.getDatabaseName(connection);
  }

}
