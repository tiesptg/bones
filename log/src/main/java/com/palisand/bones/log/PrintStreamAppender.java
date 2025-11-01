package com.palisand.bones.log;

import java.io.PrintStream;
import com.palisand.bones.log.Logger.Message;

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
