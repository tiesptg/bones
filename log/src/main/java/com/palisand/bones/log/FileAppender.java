package com.palisand.bones.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import lombok.Getter;
import lombok.Setter;

/**
 * Subclass of PrintStreamAppender that rotates the log file based on a configurable time period
 * using a background Timer. The active log file always retains the original base name. The rotated
 * file is archived using a timestamp suffix representing the start time of the period that just
 * finished.
 */

public class FileAppender extends PrintStreamAppender {

  @Setter
  @Getter
  private String logFile;
  private String currentPostfix;

  private static final Timer ROTATION_TIMER = new Timer("FileRotator");

  @Setter
  @Getter
  private ChronoUnit rotationUnit = null;

  public FileAppender() {
    super(null);
  }

  @Override
  public synchronized void init(Properties properties) throws IOException {
    super.init(properties);
    initProperty(properties, "file", value -> this.logFile = value);
    initProperty(properties, "rotation", value -> this.rotationUnit = ChronoUnit.valueOf(value));
    File file = new File(logFile);
    PrintStream out = new PrintStream(new FileOutputStream(file, true));
    setOutputStream(out);
    if (rotationUnit != null) {
      registerRotationTask();
    }
  }

  private void initCurrentPostfix() {
    String format = switch (rotationUnit) {
      case SECONDS -> ".yyyy-MM-dd.HH-mm-ss";
      case MINUTES -> ".yyyy-MM-dd.HH-mm";
      case HOURS -> ".yyyy-MM-dd.HH";
      case DAYS -> ".yyyy-MM-dd";
      case MONTHS -> ".yyyy-MM";
      case YEARS -> ".yyyy";
      default -> throw new IllegalArgumentException("Unsupported value: " + rotationUnit);
    };
    SimpleDateFormat postfixFormat = new SimpleDateFormat(format);
    currentPostfix = postfixFormat.format(new Date());
  }

  private void registerRotationTask() {
    initCurrentPostfix();
    long firstDelay = rotationUnit.getDuration().toMillis()
        - (System.currentTimeMillis() % rotationUnit.getDuration().toMillis());
    ROTATION_TIMER.scheduleAtFixedRate(new RotationTask(), firstDelay,
        rotationUnit.getDuration().toMillis());
  }

  /**
   * Internal TimerTask to handle the scheduled rotation.
   */
  private class RotationTask extends TimerTask {
    @Override
    public void run() {
      try {
        rotateFile();
      } catch (IOException e) {
        System.err.println("Unexpected failure during log file rotation ");
        e.printStackTrace();
      }
    }
  }

  private synchronized void rotateFile() throws IOException {
    if (getOutputStream() != null) {
      getOutputStream().close();
    }

    File copy = new File(logFile);
    if (copy.exists()) {
      File to = new File(copy.getParentFile(), copy.getName() + currentPostfix);
      if (!copy.renameTo(to)) {
        throw new IOException("Could not rename logfile " + logFile + " for rotation");
      }
      setOutputStream(new PrintStream(new FileOutputStream(logFile)));
    }
    initCurrentPostfix();
  }

}
