package com.palisand.log.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.palisand.bones.log.Level;
import com.palisand.bones.log.LogConfig;
import com.palisand.bones.log.Logger;

class SimpleLogTest {

  @Test
  void testSimpleMessage() {
    Logger log = Logger.getLogger(SimpleLogTest.class);
    LogConfig config = new LogConfig();
    config.setLevel(Level.ALL);
    String m = "First Message";
    config.getAppenders().add(new TestAppender(msg -> {
      assertEquals(m, msg.getMessage());
      assertEquals(Level.INFO, msg.getLevel());
    }));
    Logger.initialise(config);
    log.log(m).info();
  }

  private int count = 0;

  @Test
  void testLoggerHierarchy() {
    LogConfig config = new LogConfig();
    config.setLevel(Level.ALL);
    config.getAppenders().add(new TestAppender(msg -> count++, msg -> count++, msg -> count++,
        msg -> count++, msg -> count++, msg -> count++));
    Logger.initialise(config);
    Logger b = Logger.getLogger("a.b");
    b.setLevel(Level.INFO);
    Logger c = Logger.getLogger("a.c");
    Logger a = Logger.getLogger("a");
    a.setLevel(Level.WARN);
    String m = "hello";
    a.log(m).info();
    assertEquals(0, count);
    b.log(m).info();
    c.log(m).info();
    assertEquals(1, count);
    a.setLevel(Level.DEBUG);
    a.log(m).info();
    b.log(m).info();
    c.log(m).info();
    assertEquals(4, count);
  }

  @Test
  void testConfig() {
    System.setProperty("bones.log.file", "src/main/resources");
    Logger.initialiseLoggingSystem();
    Logger log = Logger.getLogger("x");
    log.log("test").warn();
  }

}
