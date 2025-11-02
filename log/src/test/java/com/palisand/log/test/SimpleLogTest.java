package com.palisand.log.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.palisand.bones.log.Level;
import com.palisand.bones.log.Logger;

class SimpleLogTest {

  @Test
  void testSimpleMessage() {
    System.clearProperty("bones.log.file");
    Logger config = Logger.getRootLogger();
    config.clear();
    config.setLevel(Level.ALL);
    String m = "First Message";
    config.getAppenders().add(new TestAppender(msg -> {
      assertEquals(m, msg.getMessage());
      assertEquals(Level.INFO, msg.getLevel());
    }));
    Logger log = Logger.getLogger(SimpleLogTest.class);
    log.log(m).info();
  }

  private int count = 0;

  @Test
  void testLoggerHierarchy() {
    System.clearProperty("bones.log.file");
    Logger config = Logger.getRootLogger();
    config.clear();
    config.setLevel(Level.ALL);
    config.getAppenders().add(new TestAppender(msg -> count++, msg -> count++, msg -> count++,
        msg -> count++, msg -> count++, msg -> count++));
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
    Logger.getRootLogger().clear();
    System.setProperty("bones.log.file", "src/test/resources/testlog.properties");
    Logger.initialiseLoggingSystem();
    Logger log = Logger.getLogger("x");
    log.log("test").warn();
    assertEquals(Level.ALL, Logger.getLogger("com.palisand.bones.log").getLevel());
  }

  @Test
  void testSlf4j() {
    System.clearProperty("bones.log.file");
    Logger.getRootLogger().clear();
    Logger.initialiseLoggingSystem();
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SimpleLogTest.class);
    logger.warn("this is a fake error", new NullPointerException());
  }

}
