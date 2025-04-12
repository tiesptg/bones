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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
    private final boolean mapped;

    @Setter
    @Getter
    @RequiredArgsConstructor
    public class SearchPath {
      private String name;
      private List<Attribute> fields = new ArrayList<>();
      private boolean unique = false;
    }
    
    @Setter
    @Getter
    @RequiredArgsConstructor
    public class Role {
      private Field field;
      private SearchPath foreignKey;
      private Object type;
      private Role opposite;
      private Method getter;
      private Method setter;
      
      public String getName() {
        return field.getName();
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
    }
    
    public Entity(Class<?> cls) {
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
            role.setType(getGenericType(field.getGenericType(),1));
            role.setField(field);
            role.setGetter(getGetter(cls,field));
            role.setSetter(getSetter(cls,field));
            foreignKeys.add(role);
          } else if (field.getType().isPrimitive() || isSupported(field.getType()) || !field.getType().getName().startsWith("java")) {
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
                indices.put(index.value(),path);
              }
              path.fields.add(attribute);
            }
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
  
  synchronized static Entity getEntity(Class<?> cls) {
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
    for (Class<?> type: types) {
      commands.createTable(connection,type);
    }
  }
  
  public void drop(Connection connection, Class<?>...types) throws SQLException {
    CommandScheme commands = getCommands(connection);
    for (Class<?> type: types) {
      commands.dropTable(connection,type);
    }
  }

}
