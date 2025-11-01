package com.palisand.bones.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogConfig {
  private static final Logger LOG = Logger.getLogger(LogConfig.class);
  private Level level = Level.DEBUG;
  private String name;
  private List<Appender> appenders = new ArrayList<>();
  private List<LogConfig> loggers = new ArrayList<>();

  void init(Properties properties) {
    String handlers = properties.getProperty("appenders");
    properties.remove("appenders");
    if (handlers != null) {
      String[] names = handlers.split(",");
      for (String name : names) {
        try {
          name = name.trim();
          Appender appender = (Appender) Class.forName(name).getConstructor().newInstance();
          Properties appenderProps = getPropertiesWithPrefix(properties, name + '.');
          appender.init(appenderProps);
          getAppenders().add(appender);
        } catch (Exception ex) {
          LOG.log("Could not initialise Appender").with("name", name).with(ex).warn();
        }
      }
    }
    String level = properties.getProperty("level");
    if (level != null) {
      properties.remove("level");
      try {
        this.level = Level.valueOf(level);
      } catch (Exception ex) {
        LOG.log("unknown level specification").with("level", level).warn();
      }
    }
    while (!properties.isEmpty()) {
      String key = (String) properties.propertyNames().nextElement();
      try {
        String name = key.substring(0, key.lastIndexOf('.'));
        LogConfig config = new LogConfig();
        config.setName(name);
        Properties subProperties = getPropertiesWithPrefix(properties, name + '.');
        config.init(subProperties);
        subProperties.keySet().forEach(prop -> properties.remove(prop));
      } catch (Exception ex) {
        LOG.log("illegal logger property").with("property", key).with(ex).warn();
      }
    }
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
    LogConfig config = new LogConfig();
    Properties logProps = getPropertiesWithPrefix(properties, "bones.log.");
    config.init(logProps);
    Logger.initialise(config);
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
    LogConfig config = new LogConfig();
    config.setLevel(Level.ALL);
    Appender appender = new SystemOutAppender();
    appender.setFormat("${date} ${time} ${level} [${location}] %{message}");
    config.getAppenders().add(appender);
    Logger.initialise(config);
    return true;
  }

}
