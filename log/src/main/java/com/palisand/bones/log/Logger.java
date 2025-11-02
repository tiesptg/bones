package com.palisand.bones.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;

public class Logger {
  private static final TreeMap<String, Logger> LOGGERS = new TreeMap<>();

  private final static Logger ROOT = new Logger("");
  private static final Logger LOG = Logger.getLogger(Logger.class);
  @Getter
  private final String name;
  @Getter
  private Logger parent = ROOT;
  @Setter
  @Getter
  private Level level = null;
  Message message = null;
  private List<Appender> appenders = null;

  public static Logger getLogger(Class<?> cls) {
    return getLogger(cls.getName());
  }

  public static Logger getLogger(String name) {
    synchronized (LOGGERS) {
      Logger result = LOGGERS.get(name);
      if (result == null) {
        result = new Logger(name);
        LOGGERS.put(name, result);
      }
      return result;
    }
  }

  public void clear() {
    appenders = null;
    level = null;
    if (this == ROOT) {
      synchronized (LOGGERS) {
        LOGGERS.values().forEach(logger -> logger.clear());
      }
    }
  }

  public boolean isEnabled(Level level) {
    return getActiveLevel().ordinal() >= level.ordinal();
  }

  private Level getActiveLevel() {
    if (level == null) {
      if (parent != null) {
        return parent.getActiveLevel();
      }
      return Level.ALL;
    }
    return level;
  }

  void append(Message msg) {
    if (isEnabled(msg.getLevel())) {
      sendToAppenders(msg);
    }
  }

  private void sendToAppenders(Message msg) {
    if (appenders != null) {
      appenders.forEach(appender -> appender.log(msg));
    }
    if (parent != null) {
      parent.sendToAppenders(msg);
    }
  }

  public Message log(String msg) {
    if (message != null) {
      throw new IllegalStateException("log statement not logged at " + message.getLocation());
    }
    message = new Message(this, msg);
    return message;
  }

  public static Logger getRootLogger() {
    return ROOT;
  }

  private Logger(String name) {
    this.name = name;
    SortedMap<String, Logger> submap = LOGGERS.tailMap(name);
    if (!name.isEmpty()) {
      if (!submap.isEmpty()) {
        Logger first = submap.values().iterator().next();
        if (first.getName().startsWith(name)) {
          parent = first.parent;
        }
      }
    }
    for (Entry<String, Logger> e : submap.entrySet()) {
      if (e.getKey().startsWith(name)) {
        e.getValue().parent = this;
      } else {
        break;
      }
    }
  }

  public List<Appender> getAppenders() {
    if (appenders == null) {
      appenders = new ArrayList<>();
    }
    return appenders;
  }

  void init(Properties properties) {
    String handlers = properties.getProperty("appenders");
    properties.remove("appenders");
    if (handlers != null) {
      String[] names = handlers.split(",");
      for (String name : names) {
        try {
          String cname = name.trim();
          Appender appender = (Appender) Class.forName(cname).getConstructor().newInstance();
          Properties appenderProps = getPropertiesWithPrefix(properties, cname + '.');
          appenderProps.keySet().forEach(prop -> properties.remove(cname + "." + prop));
          appender.init(appenderProps);
          getAppenders().add(appender);
        } catch (Exception ex) {
          LOG.log("Could not initialise Appender").with("name", name).with(ex).warn();
        }
      }
    }
    String level = properties.getProperty("level");
    if (level != null) {
      level = level.trim();
      properties.remove("level");
      try {
        this.level = Level.valueOf(level);
      } catch (Exception ex) {
        LOG.log("unknown level specification").with("level", level).warn();
      }
    }
    while (this == ROOT && !properties.isEmpty()) {
      String key = (String) properties.propertyNames().nextElement();
      try {
        String name = key.substring(0, key.lastIndexOf('.'));
        Logger config = Logger.getLogger(name);
        Properties subProperties = getPropertiesWithPrefix(properties, name + '.');
        subProperties.keySet().forEach(prop -> properties.remove(name + "." + prop));
        config.init(subProperties);
      } catch (Exception ex) {
        LOG.log("illegal logger property").with("property", key).with(ex).warn();
      }
    }
  }



  static {
    initialiseLoggingSystem();
  }

  public static void initialiseLoggingSystem() {
    if (initialiseFromEnvironment())
      return;
    if (initFromDefaultFile())
      return;
    initDefault();
  }

  static boolean initialiseFromEnvironment() {
    String path = System.getProperty("bones.log.file");
    if (path != null) {
      return initFromFile(path);
    }
    return false;
  }

  static boolean initFromDefaultFile() {
    return initFromFile("log.properties");
  }

  static boolean initFromFile(String path) {
    File file = new File(path);
    if (file.exists() && file.canRead()) {
      try (Reader reader = new FileReader(file)) {
        Properties properties = new Properties();
        properties.load(reader);
        initFromProperties(properties);
        return true;
      } catch (IOException ex) {
        LOG.log("Error while reading logging config").with("file-name", file.getAbsolutePath())
            .with(ex).warn();
      }
    }
    try (InputStream in = Logger.class.getResourceAsStream(path)) {
      if (in != null) {
        Properties properties = new Properties();
        properties.load(in);
        initFromProperties(properties);
        return true;
      }
    } catch (IOException ex) {
      LOG.log("Error while reading logging config from classpath")
          .with("file-name", file.getAbsolutePath()).with(ex).warn();
    }
    return false;
  }

  static void initFromProperties(Properties properties) {
    Properties logProps = getPropertiesWithPrefix(properties, "bones.log.");
    ROOT.init(logProps);
  }

  static Properties getPropertiesWithPrefix(Properties properties, String prefix) {
    Properties result = new Properties();
    for (String key : properties.stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        result.put(key.substring(prefix.length()), properties.getProperty(key));
      }
    }
    return result;
  }

  static boolean initDefault() {
    Appender appender = new SystemOutAppender();
    appender.setFormat("${date} ${time} ${level} [${location}] ${message}");
    ROOT.getAppenders().add(appender);
    ROOT.setLevel(Level.ALL);
    return true;
  }



}
