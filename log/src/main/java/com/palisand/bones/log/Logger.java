package com.palisand.bones.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

public class Logger {
  private static final TreeMap<String, Logger> LOGGERS = new TreeMap<>();

  private static final Logger LOG = Logger.getLogger(Logger.class);
  private final static Logger ROOT = new Logger("");
  @Getter
  private final String name;
  @Getter
  private Logger parent;
  @Setter
  @Getter
  private Level level = null;
  private Message message = null;
  private List<Appender> appenders = null;

  @Getter
  public class Message {
    private final Supplier<String> message;
    private final String location;
    private final Instant timestamp;
    private Throwable throwable;
    private Level level;
    private TreeMap<String, Object> fields;

    Message(String msg) {
      this(() -> msg);
    }

    Message(Supplier<String> msgSupplier) {
      message = msgSupplier;
      StackTraceElement stack = Thread.currentThread().getStackTrace()[1];
      location = stack.getClassName() + ":" + stack.getLineNumber();
      timestamp = Instant.now();
    }

    public String getDate() {
      return LocalDate.from(timestamp).toString();
    }

    public String getTime() {
      return timestamp.toString().substring(15, 28);
    }

    public String getMessageString() {
      StringWriter sb = new StringWriter();
      if (message != null) {
        sb.append(message.get());
      }
      if (fields != null) {
        sb.append(fields.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(" ")));
      }
      if (throwable != null) {
        try (PrintWriter out = new PrintWriter(sb)) {
          throwable.printStackTrace(out);
        }
      }
      return sb.toString();
    }

    public Message with(String name, Object value) {
      if (fields == null) {
        fields = new TreeMap<>();
      }
      fields.put(name, value);
      return this;
    }

    public Message with(Throwable throwable) {
      this.throwable = throwable;
      return this;
    }

    public String getMessage() {
      return message.get();
    }

    public void fatal() {
      log(Level.FATAL);
    }

    public void error() {
      log(Level.ERROR);
    }

    public void warn() {
      log(Level.WARN);
    }

    public void info() {
      log(Level.INFO);
    }

    public void debug() {
      log(Level.DEBUG);
    }

    public void trace() {
      log(Level.TRACE);
    }

    private void log(Level level) {
      this.level = level;
      Logger.this.message = null;
      append(this);
    }

  }

  public static Logger getLogger(Class<?> cls) {
    return getLogger(cls.getName());
  }

  public synchronized static Logger getLogger(String name) {
    Logger result = LOGGERS.get(name);
    if (result == null) {
      result = new Logger(name);
      LOGGERS.put(name, result);
    }
    return result;
  }

  private void copyFrom(LogConfig config) {
    appenders = config.getAppenders();
    level = config.getLevel();
    config.getLoggers().forEach(logConfig -> {
      final Logger logger = Logger.getLogger(logConfig.getName());
      logger.copyFrom(logConfig);
    });
  }

  public static void initialise(LogConfig config) {
    ROOT.copyFrom(config);
  }

  private boolean isEnabled(Level level) {
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

  private void append(Message msg) {
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
    message = new Message(msg);
    return message;
  }

  public static Logger getRootLogger() {
    return ROOT;
  }

  private Logger(String name) {
    this.name = name;
    SortedMap<String, Logger> submap = LOGGERS.tailMap(name);
    parent = submap.isEmpty() ? ROOT : submap.values().iterator().next();
    for (Entry<String, Logger> e : submap.entrySet()) {
      if (e.getKey().startsWith(name)) {
        e.getValue().parent = this;
      } else {
        break;
      }
    }
  }

  static {
    initialiseLoggingSystem();
  }

  private static void initialiseLoggingSystem() {
    @SuppressWarnings("unused")
    boolean initialised = LogConfig.initialiseFromEnvironment() || LogConfig.initFromDefaultFile()
        || LogConfig.initDefault();

  }

}
