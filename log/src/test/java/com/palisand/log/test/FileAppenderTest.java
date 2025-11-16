package com.palisand.log.test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.palisand.bones.log.Logger;

@Disabled
class FileAppenderTest {
  private static final Logger LOG = Logger.getLogger(FileAppenderTest.class);

  private static final String CONFIG = """
      bones.log.appenders=com.palisand.bones.log.FileAppender
      bones.log.level=INFO
      bones.log.com.palisand.bones.log.FileAppender.file=target/log/test.log
      bones.log.format=${date} ${time} ${level} ${location}> ${message}
      bones.log.com.palisand.bones.log.FileAppender.rotation=SECONDS
      bones.log.com.palisand.level=ALL
      """;

  @Test
  void test() throws IOException, InterruptedException {
    Properties properties = new Properties();
    properties.load(new StringReader(CONFIG));
    File logdir = new File("target/log");
    logdir.mkdirs();
    Logger.getRootLogger().clear();
    Logger.initFromProperties(properties);
    int times = 50;
    for (int i = 0; i < times; ++i) {
      LOG.log("test message").with("count", i).info();
      Thread.sleep(100);
    }
    System.out.println("found " + logdir.list().length + " logfiles");
  }

}
