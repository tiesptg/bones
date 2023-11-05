# Bones
Bones is bare bones dependency injection framework. That can replace spring-context or Google Guice.
It is meant to be mean and lean. It contains of only one class and a interface, but supports full dependencies
including bidirectional or circular dependencies. It does not use classpath scanning of annotation magic. 
It does not use reflection. All code is simple and transparant.

# Usage
At the moment this library is not yet registered in Maven Central, so you will have to build and install it
in your local maven repository

First clone this repository and run `mvn install`.

To use it, put the following dependency in your Maven project file (pom.xml).

```
<dependency>
  <groupId>com.palisand</groupId>
  <artifactId>bones</artifactId>
  <version>0.9</version>
</dependency>
```

Each class that needs dependency injection or may be injected as dependency should implement
the Injectable interface

```
import com.palisand.bones.Injectable;
import com.palisand.bones.ApplicationContext;

public class MyBean implements Injectable {
  Repository repository;
  Service service
  ...
  public void injectFrom(ApplicationContext ctx) {
    this.repository = ctx.get(Repository.class);
    this.service = ctx.get("MyService");
    ...
  }
  ...
}
```

The application supports types and named components.

Create one subclass of ApplicationContext that will register all your managed components

```
import com.palisand.bones.Injectable;
import com.palisand.bones.ApplicationContext;

public class MyApplicationContext extends ApplicationContext {
  public MyApplicationContext() {
    // registration of typed components
    register(new Repository("Any config item"),new MyBean());
    // registration of named components
    register("MyService",new MyService());
  }
}
```

Now you can use your beans in your application

```
public class Main(String...args) {

  public static void main(String...args) {
    ApplicationContext ctx = new MyApplicationContext();
    // this bean will now contain a repository and a service component
    MyBean bean = ctx.get(MyBean.class);
    ...
  }

}
```
# Why doesn't Bones implement JSR 330
I like a library to have one clear method of doing things. JSR 330 offers many options and all of them have drawbacks.
This means you have to decide if you want to use constructor, field or setter injection. In bones there is only one way.

JSR330 is not possible without annotation processing and reflection. I want to avoid both in this library.

# Migrate from Spring Context to Bones
1. ***Change your managed components marked by spring annotation to Injectables:*** Remove the annotations,implement the Injectable interface and its injectFrom method.
2. ***Transform your Configuration class to a subclass of ApplicationContext:*** In this class you should register all your components from step 1 and any beans you declared in you configuration
3. ***Use an instance of your ApplicationContext class instead of the spring-context provided AppliationContext.*** Change the getBean methods to get.


