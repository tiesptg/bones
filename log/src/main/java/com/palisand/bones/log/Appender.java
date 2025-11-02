package com.palisand.bones.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Appender {
  private static final Logger LOG = Logger.getLogger(Appender.class);
  private Level level = null;
  private String format;
  private List<Function<Message, String>> fields;

  public void setFormat(String pattern) {
    StringBuilder result = new StringBuilder(pattern);
    Matcher m = Pattern.compile("\\$\\{(\\w+)\\}").matcher(pattern);
    ArrayList<Function<Message, String>> list = new ArrayList<>();
    int offset = 0;
    while (m.find()) {
      String replacement = "%s";
      String label = m.group(1);
      if (label.equals("level")) {
        replacement = "%5s";
        list.add(msg -> msg.getLevel().name());
      } else if (label.equals("date")) {
        list.add(msg -> msg.getDate());
      } else if (label.equals("time")) {
        list.add(msg -> msg.getTime());
      } else if (label.equals("location")) {
        list.add(msg -> msg.getLocation());
      } else if (label.equals("message")) {
        list.add(msg -> msg.getMessageString());
      }
      result.replace(m.start() - offset, m.end() - offset, replacement);
      offset += m.end() - m.start() - replacement.length();
    }
    format = result.toString();
    fields = list;
  }

  protected String formatMessage(Message msg) {
    return String.format(format, fields.stream().map(f -> f.apply(msg)).toArray());
  }

  public boolean isEnabled(Level level) {
    return this.level == null || this.level.ordinal() >= level.ordinal();
  }

  public abstract void log(Message msg);

  void init(Properties properties) {
    String strLevel = properties.getProperty("level");
    if (strLevel != null) {
      strLevel = strLevel.trim();
      try {
        level = Level.valueOf(strLevel);
      } catch (Exception ex) {
        LOG.log("invalid value for level").with("value", strLevel).with(ex).warn();
      }
    }
    String format = properties.getProperty("format");
    if (format != null) {
      try {
        setFormat(format);
      } catch (Exception ex) {
        LOG.log("invalid value for format").with("value", strLevel).with(ex).warn();
      }
    }
  }
}
