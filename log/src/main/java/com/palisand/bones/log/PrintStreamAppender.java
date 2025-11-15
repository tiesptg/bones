package com.palisand.bones.log;

import java.io.PrintStream;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrintStreamAppender extends Appender {

  private PrintStream outputStream;

  public PrintStreamAppender(PrintStream stream) {
    outputStream = stream;
  }

  @Override
  public void log(Message msg) {
    if (isEnabled(msg.getLevel())) {
      synchronized (this) {
        outputStream.println(formatMessage(msg));
      }
    }
  }

}
