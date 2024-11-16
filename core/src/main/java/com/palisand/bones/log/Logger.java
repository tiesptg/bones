package com.palisand.bones.log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.Setter;

public class Logger {
	
	public enum Level {
		FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL
	}
	
	private static final Map<String,Logger> LOGGERS = new TreeMap<>();
	private static final Logger ROOT = new Logger("");
	@Getter private final String name;
	@Getter private final Logger parent;
	@Setter @Getter private Level level = null;
	private Message message = null;
	private List<Appender> appenders = null;
	
	@Getter
	public class Message {
		private final Supplier<String> message;
		private final String location;
		private final Instant timestamp;
		private Throwable throwable;
		private Level level;
		private TreeMap<String,Object> fields;
		
		Message(String msg) {
			this(() -> msg);
		}
		
		Message(Supplier<String> msgSupplier) {
			message = msgSupplier;
			StackTraceElement stack = Thread.currentThread().getStackTrace()[1];
			location = stack.getClassName() + ":" + stack.getLineNumber();
			timestamp = Instant.now();
		}
		
		public Message with(String name, Object value) {
			if (fields == null) {
				fields = new TreeMap<>();
			}
			fields.put(name,value);
			return this;
		}
		
		public Message with(Throwable throwable) {
			this.throwable = throwable;
			return this;
		}
		
		public String getMessage() {
			return message.get();
		}
		
		public void fatal() {
			log(Level.FATAL);
		}
		
		public void error() {
			log(Level.ERROR);
		}
		
		public void warn() {
			log(Level.WARN);
		}
		
		public void info() {
			log(Level.INFO);
		}
		
		public void debug() {
			log(Level.DEBUG);
		}
		
		public void trace() {
			log(Level.TRACE);
		}
		
		private void log(Level level) {
			this.level = level;
			Logger.this.message = null;
			append(this);
		}
		
	}
	
	public static Logger getLogger(Class<?> cls) {
		return getLogger(cls.getName());
	}
	
	public static Logger getLogger(String name) {
		Logger result = LOGGERS.get(name);
		if (result == null) {
			result = new Logger(name);
			LOGGERS.put(name, result);
		}
		return result;
	}
	
	public static void initialise(LogConfig config) {
		ROOT.setLevel(config.getLevel());
		config.getAppenders().forEach(appender -> ROOT.getAppenders().add(appender));
		config.getLoggers().forEach(logConfig -> {
			final Logger logger = Logger.getLogger(logConfig.getName());
			logger.setLevel(logConfig.getLevel());
			logConfig.getAppenders().forEach(appender -> logger.appenders.add(appender));
		});
	}
	
	private List<Appender> getAppenders() {
		if (appenders == null) {
			appenders = new ArrayList<>();
		}
		return appenders;
	}
	
	private boolean isEnabled(Level level) {
		return level == null || this.level.ordinal() >= level.ordinal();
	}
	
	private void append(Message msg) {
		if (appenders != null && isEnabled(msg.getLevel())) {
			appenders.forEach(appender -> appender.log(msg));
		}
		if (parent != null) {
			parent.append(msg);
		}
	}
	
	public Message log(String msg) {
		if (message != null) {
			throw new IllegalStateException("log statement not logged at " + message.getLocation());
		}
		message = new Message(msg);
		return message;
	}
	
	public static Logger getRootLogger() {
		return ROOT;
	}
	
	public Logger(String name) {
		Logger parent = null;
		while (parent == null) {
			int pos = name.lastIndexOf('.');
			if (pos != -1) {
				name = name.substring(0,pos);
				parent = LOGGERS.get(name);
			} else {
				parent = ROOT;
				break;
			}
		}
		this.parent = parent;
		this.name = name;
		LOGGERS.put(name, this);
	}
	
	static {
		LogConfig logConfig = new LogConfig();
		logConfig.getAppenders().add(new SystemOutAppender());
		initialise(logConfig);
	}
	
}
