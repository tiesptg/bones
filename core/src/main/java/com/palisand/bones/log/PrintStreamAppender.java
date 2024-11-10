package com.palisand.bones.log;

import java.io.PrintStream;
import java.util.stream.Collectors;

import com.palisand.bones.log.Logger.Message;

import lombok.Getter;
import lombok.Setter;

public class PrintStreamAppender extends Appender {

	private final PrintStream out;
	@Getter private String pattern;
	private String[] patternParts = null;
	
	public PrintStreamAppender(PrintStream stream) {
		out = stream;
	}
	
	public void setPattern(String pattern) {
		this.pattern = pattern;
		this.patternParts = pattern.split("${|}");
	}
	
	
	
	@Override
	public void log(Message msg) {
		if (isEnabled(msg.getLevel())) {
			String line = String.format("%s %5s %s: %s %s", msg.getTimestamp(),msg.getLevel(),msg.getLocation(),msg.getMessage(),msg.getFields() == null ? "" : msg.getFields().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(" ")));
			out.println(line);
			if (msg.getThrowable() != null) {
				msg.getThrowable().printStackTrace(out);
			}
		}
	}
	
	public static void main(String...args) {
		String p = "${timestamp} ${level} ${location}: ${message}";
	}
	
	
	

}
