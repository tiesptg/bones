package com.palisand.log.test;

import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.function.Consumer;
import com.palisand.bones.log.Logger.Message;
import com.palisand.bones.log.SystemOutAppender;

public class TestAppender extends SystemOutAppender {
  private final Queue<Consumer<Message>> checks;

  @SafeVarargs
  public TestAppender(Consumer<Message>... checks) {
    setFormat("${date} ${time} ${level} [${location}] ${message}");
    this.checks = new ArrayDeque<>(Arrays.asList(checks));
  }

  @Override
  public void log(Message msg) {
    super.log(msg);
    Consumer<Message> checker = checks.poll();
    if (checker == null) {
      fail("test error no check available for this test message");
    }
    checker.accept(msg);
  }

}
