# The bones-log library: The leanest logging library

The bones-log library is a replacement for other logging libraries like log4j, logback and java.util.logging. It fully supports Slf4j and can serialize messages in structure forms like JSON or YAML.
As all bones libraries it minimizes its dependencies and for this library it requires only lombok at compile time. For Slf4j support you need to add the slf4j-api jar to your classpath explicitely.

## create a logger

To create a logger add a private static final logger to your class

```
public class MyClass {
  private static final Logger LOG = Logger.getLogger(MyClass.class);
...
}
```

In most cases this is the best approach. In certain cases you may not want to use the classname as Logger ID. Than you can use any string and identification of the logger.

## Log statements

A log statement works slightly different than other libraries. It works with a builder pattern.

```
LOG.log("message").with("field","value).with("field2","value2").with(exception).warn();
```

With the 'with' methods you add fields or one exception to a message and you send it with to level method to the logger to be processed by the appenders.
Next to the warn method used here, you can use 'trace', 'debug', 'info', 'error' and 'fatal'

## Configuration

The configuration process consists of 3 steps:
1. Check the bones.log.file system property to see if a custom configuration file is set.
2. if the bones.log.file system property is not found it will assume the default filename "log.properties"
3. If a logfile is not found, the default configuration is activated

When the library has determined the path to a configuration file. It will first look on the filesystem and then in the classpath to find it. If both are not found it will proceed to the next step.

If you want to configure in code, you will always have a configuration in place that you can change or clear completely.


### In code

The library is by default configured to log on debug level to System.out. This will help you set up a first test environment without having to write configuration files

When you want to change the default configuration in code, you can do something like this:

```
Logger.getRootLogger().setLevel(Level.INFO);
```
This will suppress any log messages with a level lower than INFO.

To overwrite the current configuration, you can use something like this:

```
Logger config = Logger.getRootLogger();
config.clear();
FileAppender appender = new FileAppender();
appender.setLogFile("log/application.log");
appender.setRotationUnit(ChronoUnit.DAYS);
config.getAppenders().add(appender);
config.setFormat("${date} ${time}: [${level}] ${location} = ${message}");
Logger.getLogger("com.palisand").setLevel(Level.ALL);
```

This example first gets the root logger that specifies that contains the configuration for all loggers. The clear method clears the old configuration.
Line 3 through 6 creates and initializes a File appender that rotates daily and adds it to the configuration. With the setFormat method you can set the format for logging
This is the default format for all appenders, but an appender can override this and may choose a whole different format like JSON.
The last line changes the log lever for all classes in the packages starting with "com.palisand" to ALL.

### With configuration in a property file

You can use a property file to configure the logging system. All properties should start with 'bones.log.'

```
bones.log.appenders=com.palisand.bones.log.FileAppender
bones.log.level=INFO
bones.log.com.palisand.bones.log.FileAppender.file=target/log/test.log
bones.log.format=${date} ${time} ${level} ${location}> ${message}
bones.log.com.palisand.bones.log.FileAppender.rotation=SECONDS
bones.log.com.palisand.level=ALL
```

The bones.log.appenders property is a comma separated list of the full name of the appenders to use.
The properties with 'bones.log.<full name of appender>.property' refers to a property of an appender. These properties are given to the appender to configure itself.
The properties with 'bones.log.<logger name>.level' or 'bones.log.<logger name>.format' can be used to change configuration of loggers with this name or prefix.

## Make your own appender

To make your own appender just subclass it from com.palisand.bones.log.Appender and implement the init and log methods

```

public class MyAppender extends Appender {
  private String property;

  @Override
  public void init(Properties properties) {
    property = properties.get("property");
  }
  
  @Override
  public void log(Message msg) {
    if (isEnabled(msg.getLevel())) {
      synchronized (this) {
        sendToMessage(formatMessage(msg));
      }
    }
  }
}
```

In the init method you will get a properties object that contains the properties that are relevant for you appender. The prefix is already stripped from it.

In the log method you send the message to the medium of your appender. If this medium needs synchronization - as it often does - you have to make sure your appender is threadsafe

## Use bones-log as a back-end for Slf4j

If you are using Slf4j - or a library you use is using it - bones-log will be picked up by Slf4j as its backend. If you want to use it in your own application, just add the slf4j-api jar to your dependencies and it will work automatically