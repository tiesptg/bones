package com.palisand.bones.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Subclass of PrintStreamAppender that rotates the log file based on a configurable time period
 * using a background Timer. The active log file always retains the original base name. The rotated
 * file is archived using a timestamp suffix representing the start time of the period that just
 * finished.
 */
public class FileAppender extends PrintStreamAppender {

  // SimpleDateFormats for generating the archive suffix
  private final SimpleDateFormat archiveDateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private final SimpleDateFormat archiveDateTimeFormat =
      new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

  private String baseFileName;

  // Fields for time-based rotation
  private final long intervalMillis;
  private static final Timer rotationTimer = new Timer("FileRotator");

  // Stores the timestamp (formatted string) of when the CURRENT outputStream was opened.
  private String streamStartTimeSuffix = "";

  // Stores the time unit used for rotation (to decide which format to use)
  private final TimeUnit rotationUnit = TimeUnit.DAYS;

  /**
   * Constructs the appender, setting the base file name and rotation period. * @param baseFileName
   * The base name for the log file (e.g., "app.log").
   * 
   * @param rotationPeriod The duration of the logging period (e.g., 1).
   * @param unit The time unit for the duration (e.g., TimeUnit.HOURS).
   */
  @Override
  public void init(Properties properties) {
    // Pass a dummy stream to the super constructor initially
    super.init(properties);
    initProperty(properties, "basefile", value -> this.baseFileName = value);
    initProperty(properties, "rotation", value -> TimeUnit.valueOf(value));


    // 1. Open the initial stream
    initNewStream();

    // 2. Schedule the rotation task to run at fixed intervals
    // The first execution will happen after 'intervalMillis'
    this.rotationTimer.scheduleAtFixedRate(new RotationTask(), intervalMillis, intervalMillis);
  }

  /**
   * The core log method. Now just logs, rotation is handled by the Timer thread.
   */
  @Override
  public void log(Message msg) {
    if (isEnabled(msg.getLevel())) {
      // Synchronization is crucial for thread safety during logging AND rotation
      // The log() method is synchronized on 'this', and the TimerTask synchronizes
      // on 'this' as well, preventing concurrent log writes and rotations.
      synchronized (this) {
        // Call the parent's logic to actually print the message
        super.log(msg);
      }
    }
  }

  /**
   * Internal TimerTask to handle the scheduled rotation.
   */
  private class RotationTask extends TimerTask {
    @Override
    public void run() {
      // Synchronize on the appender instance to block logging threads during rotation
      synchronized (FileAppender.this) { // dont do this
        try {
          System.out.println("TIMER TRIGGERED: Starting log rotation.");
          rotateFile();
          initNewStream();
        } catch (IOException e) {
          System.err.println("FATAL: Timer rotation failed: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Closes the existing stream (baseFileName) and renames it to include the suffix of the time
   * period that just finished (stored in streamStartTimeSuffix).
   */
  private void rotateFile() {
    // 1. Close the existing stream safely to release the file lock
    super.close();

    // 2. Get the unique suffix of the file we just closed
    if (streamStartTimeSuffix.isEmpty()) {
      return;
    }

    // Example: "app.log" -> "app-2025-11-15_14-30-00.log"
    String rotatedFileName = baseFileName.substring(0, baseFileName.lastIndexOf('.')) + "-"
        + streamStartTimeSuffix + baseFileName.substring(baseFileName.lastIndexOf('.'));

    File currentFile = new File(baseFileName);
    File rotatedFile = new File(rotatedFileName);

    // 3. Perform the rename operation
    if (currentFile.exists() && currentFile.renameTo(rotatedFile)) {
      System.out.println("LOG ROTATED: Renamed " + baseFileName + " to " + rotatedFileName);
    } else {
      // This can happen if the file is still locked by the OS or another process.
      System.err.println("WARNING: Could not rename log file: " + baseFileName + " to "
          + rotatedFileName + ". Rotation skipped.");
    }
  }

  /**
   * Opens a new PrintStream pointing to the original baseFileName and updates the rotation tracking
   * variables.
   */
  private void initNewStream() throws IOException {
    Date now = new Date();

    // Update the unique suffix based on the rotation period granularity.
    // Use full date/time if rotation is more frequent than daily.
    if (rotationUnit.ordinal() < TimeUnit.DAYS.ordinal()) {
      streamStartTimeSuffix = archiveDateTimeFormat.format(now);
    } else {
      streamStartTimeSuffix = archiveDateFormat.format(now);
    }

    // 1. Open the new file stream pointing to the baseFileName.
    // We use append=true to handle application restart within the same period gracefully.
    FileOutputStream fos = new FileOutputStream(baseFileName, true);
    setOutputStream(new PrintStream(fos));

    System.out.println("LOG INITIALIZED: Opened current log file: " + baseFileName
        + " (Archive Suffix: " + streamStartTimeSuffix + ")");
  }

  /**
   * Overrides the close method to ensure the current stream is closed, and the Timer is cancelled
   * when the application shuts down.
   */
  @Override
  public void close() {
    // 1. Cancel the timer to stop future rotations
    if (rotationTimer != null) {
      rotationTimer.cancel();
      rotationTimer.purge();
    }
    // 2. Close the stream
    super.close();
    System.out.println("DailyRollingFileAppender closed and Timer cancelled.");
  }
}
