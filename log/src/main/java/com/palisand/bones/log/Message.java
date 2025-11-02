package com.palisand.bones.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class Message {
  /**
   * 
   */
  private final Logger logger;
  private final Supplier<String> message;
  private final String location;
  private final Instant timestamp;
  private Throwable throwable;
  private Level level;
  private TreeMap<String, Object> fields;

  Message(Logger logger, String msg) {
    this(logger, () -> msg);
  }

  Message(Logger logger, Supplier<String> msgSupplier) {
    this.logger = logger;
    message = msgSupplier;
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    int i = 1;
    while (i < stack.length - 1
        && (stack[i].getClassName().startsWith(Message.class.getPackageName())
            || stack[i].getClassName().toLowerCase().contains("slf4j")
            || stack[i].getClassName().toLowerCase().contains("logging"))) {
      ++i;
    }
    location = stack[i].toString();
    timestamp = Instant.now();
  }

  public String getDate() {
    return timestamp.toString().substring(0, 10);
  }

  public String getTime() {
    return timestamp.toString().substring(11, 23);
  }

  public String getMessageString() {
    StringWriter sb = new StringWriter();
    if (message != null) {
      sb.append(message.get());
    }
    if (fields != null) {
      sb.append(" ");
      sb.append(fields.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue())
          .collect(Collectors.joining(" ")));
    }
    if (throwable != null) {
      sb.append("\n");
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
    this.logger.message = null;
    this.logger.append(this);
  }

}
