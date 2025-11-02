package com.palisand.bones.log;

import java.io.PrintStream;

public class PrintStreamAppender extends Appender {

  private final PrintStream out;

  public PrintStreamAppender(PrintStream stream) {
    out = stream;
  }

  @Override
  public void log(Message msg) {
    if (isEnabled(msg.getLevel())) {
      out.println(formatMessage(msg));
    }
  }
}
