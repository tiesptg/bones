package com.palisand.bones.log.slf4j;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import com.palisand.bones.log.Message;

public class BonesLogger extends AbstractLogger {
  private static final long serialVersionUID = 8897081851691172239L;
  private final com.palisand.bones.log.Logger logger;

  public BonesLogger(String name) {
    logger = com.palisand.bones.log.Logger.getLogger(name);
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isEnabled(com.palisand.bones.log.Level.TRACE);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return false;
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isEnabled(com.palisand.bones.log.Level.DEBUG);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return false;
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isEnabled(com.palisand.bones.log.Level.INFO);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return false;
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isEnabled(com.palisand.bones.log.Level.WARN);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return false;
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isEnabled(com.palisand.bones.log.Level.ERROR);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return false;
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return logger.getName();
  }

  @Override
  protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern,
      Object[] arguments, Throwable throwable) {
    String msg = messagePattern;
    if (arguments != null && arguments.length != 0) {
      msg = String.format(messagePattern.replace("{}", "%s"), arguments);
    }
    Message message = logger.log(msg);
    if (throwable != null) {
      message.with(throwable);
    }
    switch (level) {
      case TRACE:
        message.trace();
        break;
      case DEBUG:
        message.debug();
        break;
      case INFO:
        message.info();
        break;
      case WARN:
        message.warn();
        break;
      case ERROR:
        message.error();
        break;
    }
  }

}
