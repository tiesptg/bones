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
    if (handlers != null) {
      String[] names = handlers.split(",");
      for (String name : names) {
        try {
          Appender appender = (Appender) Class.forName(name).getConstructor().newInstance();
          Properties appenderProps = getPropertiesWithPrefix(properties, name + '.');
          appender.init(appenderProps);
          getAppenders().add(appender);
        } catch (Exception ex) {
          LOG.log("Could not initialise Appender").with("name", name).with(ex).warn();
        }
      }
    }
  }

  static boolean initialiseFromEnvironment() {
    return false;
  }

  static boolean initFromDefaultFile() {
    String defaultFile = "log.properties";
    File file = new File(defaultFile);
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
    try (InputStream in = Logger.class.getResourceAsStream(defaultFile)) {
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
  }

  static Properties getPropertiesWithPrefix(Properties properties, String prefix) {
    Properties result = new Properties();
    for (String key : properties.stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        result.put(key.substring(key.length()), properties.getProperty(key));
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
