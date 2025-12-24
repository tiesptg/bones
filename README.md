# The bones library suite of Palisand

This library suite contains libraries for common tasks in applications. I know they are equivalents of well known libraries that many use. But this project gives me the opportunity to create alternatives that I think are better. The libraries do not rely on many dependencies, so the resulting jars are tiny and can be used in any contexts.

## bones-core: Dependency injection framework

[bones-core](https://github.com/tiesptg/bones/tree/main/core) is an alternative to Spring context or Google Juice. It is extreme simple and powerful. It consists of just one class and one interface. It does not rely on Annotations and classpath scanning. But you may use it if you wish to. The bones-validation framwork contains some classes that can help with it. With this library there is not multiple ways to do dependency injection, it only supports one method and that will do in all cases. As added bonus, it will not be bothered by cycles in dependencies. It will handle it without any problems.

## bones-log: Logging framework

[bones-log](https://github.com/tiesptg/bones/tree/main/log) is an alternative to log4j, logback and java.util.logging, etc. The log messages consist of map structure, so you can add custom fields and serialize them as json or yaml objects. It only contains a rolling file based appender and a System.out appender, but it is very simple to build your own and make it fully configurable through the mechanism of the library. By default the library is fully configured to log to System.out, so if you want to test it, it works without a configuration file. You can configure it through code or a property file. I like this because you can include the properties in the property file you already use in your application. It will ignore any properties it does not use. It also supports Slf4j, just add the slfj2-api jar on the classpath it will work. If you are using jboss-logging it will work through its Slf4j option.

## bones-validation: Validation framework

[bones-validation](https://github.com/tiesptg/bones/tree/main/validation) is an alternative for hibernate-validation. It is also very similar to Hibernate Validation because it uses a lot of similar annotations. Some annotations though do not only check constraints, but may also correct a value. The @UpperCase annotation will uppercase the value of the field carrying it and there are many more like it. It is also fully extendible, so it is very easy to add your own validation rules (or corrections).

## bones-persist: Persistence framework

[bones-persist](https://github.com/tiesptg/bones/tree/main/persist) is an alternative to Hibernate ORM or Toplink. It is an object relational mapper library that supports most features of Hibernate, but gives you more control. It is very efficient. It will not create implicit select statements, does not have the common Hibernate issues of Lazy initialization errors and others. And that all in just 9 classes and no dependencies beside jdbc drivers. It supports Oracle, MS SqlServer, MySql and PostgreSQL.

## bones-meta: Meta programming framework

[bones-meta](https://github.com/tiesptg/bones/tree/main/persist) is a meta programming library. This library is not a replacement of a well known open source framework. If you know Eclipse Modeling than you will recognize some of its features, but it is more powerful. You can compare a metamodel with a DSL, but it does not use a grammar to specify you model. With this library you can create your own metamodels. It contains a maven plugin that can use meta models that can be used to generate code and a Swing GUI to edit the metamodels. The metamodels are stored in TypedText files that are typed yaml files. The library contains a fully working parser/generator functions for TypedText as Gson and Jackson are for Json and Yaml. The files may have links between files to create a large repository of metadata. By using files, you can store the metadata of your application with the source code in a source code repository like git. It is created with itself so it implements actually a meta-meta model to create meta models. It uses the same GUI to edit metamodels and generates code with the same maven plugin.