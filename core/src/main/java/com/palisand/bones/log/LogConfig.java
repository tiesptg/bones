package com.palisand.bones.log;

import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.log.Logger.Level;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogConfig {
	private Level level = Level.DEBUG;
	private String name;
	private List<Appender> appenders = new ArrayList<>();
	private List<LogConfig> loggers = new ArrayList<>();
}
