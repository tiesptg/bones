# Bare Bones Persist Library

This library is an object relational mapping library. It supports a full set of features comparable to Hibernate (https://hibernate.org/orm/), but has a different design goal. The Persist Library of Bare Bones Suite gives the programmer more control over the SQL it uses to store or read data. In almost all instances each call into the library will use at most one SQL statement. There are no implicit selects or inserts/updates of related objects.
A database query will always be one select statement.

The second design goal is to promote proven strategies to handle database logic. It fully supports generated surrogate keys and object versioning for the optimistic locking strategy. Both are always a good method, but they are not obligated. The library will work without versions and functional primary keys. It also supports primary keys of more then one field.

The Query does force paging of data. This means a Query will never return more than a predetermined number of rows in one page. But you can loop through pages to retrieve all rows if necessary.

The library also requires the use of transactions. It will allways disable the autocommit mode of jdbc connections, but it offers a simple transaction through lambda functions.

During a transaction all instances of classes will be cached and the library guarantees that a fully selected object will always be the same as earlier objects retrieved by the library . It is not necessary to link an object to a database object before using it. It is often not necessary to have a complete object. In the the delete or refresh method of Database, the library will only look at the primary key values and version values if you use that mechanism.

*WARNING: the library is still in alpha state although the presented feature are tested and working, you may be confronted with errors or unimplemented features. You are encouraged to file an issue in the github project. The Api may also change during further development. So do not use in production environments yet*

## Supported features

- Single field primary keys and compound primary keys
- Autoincrementing fields as primary key fields
- Object versioning and full support of the optimistic locking strategy
- Object inheritance with fully supported polymorphism
- 1-to-1 and 1-to-many relationships through foreignkeys constraints with indices.
- many-to-many relationships through an explicit relationship object
- automatic creation of tables and automatic upgrades for many scenarios
- Flexible query mechanism to select one or more objects or function results in one statement
- Simple transaction logic with object caching so each object will have only one instance during a transction.

## Supported Databases

The library has been tested with:
- Oracle 23c
- MS SQL Server 2019
- PostgreSQL 17.5
- MySQL 8.0
- H2 as testing database.

# Getting started

Clone the main branch of this library.

`git clone https://github.com/tiesptg/bones.git`

Use maven to build the library and install it in your local repository

`mvn clean install`

Add the library to your pom.xml as a dependency:

```
<dependencies>
  ...
  <dependency>
  	<groupId>com.palisand.bones</groupId>
	<artifactId>bones-persist</artifactId>
	<version>0.9</version>
  </dependency>
  ...
</dependencies>
```

The library itself only uses Lombok (https://projectlombok.org/) as a dependency. We don't call it bare bones for nothing.

## Create your persistent model

The persistent model for Bare Bones Persist are simple Pojo's with some annotations very similar to entity beans. We advise you to use Lombok to generate boilerplate code.
Let me give you an example

```
@Data
public class Person {

  @Id(generated=true)
  private long oid;
  @Version 
  private int oversion;
  private String name;
  private LocalDate birthday;

}
```

The Data annotations is from Lombok. The Id annotation indicates the field is (part of) the primary key. More than one field can have this annotation. All fields with it will be used as primary key. If you have no Id annotations, the table will not be generated with a primary key.

The Version annotation indicates a field that will be used for the optimistic locking strategy. This means that when you update or delete an object which version field is out of sync with the database, the library will throw a StaleObjectException. This indicates the object should be refreshed and send to the user to let him verify that the change he intended should happen. The library will update the version field at each update.

The library supports most primitive datatypes and its Class equivalents. The Class equivalents are considered nullable and the primitive types not nullable. It supports java.sql.Date and Time and some java.time.* classes. BigDecimal and BigInteger fields. java.sql.Clob and -Blob field are supported for large dataitems. String and byte[] will also support any length that will fit in memory.

Any annotations of the library are declared within the Database class. Checkout the javadoc or souce code for more information.

### Relations

Any field with a type that references another persistent class will be interpreted as relation. A single class is the one side of a relation, a List or other type of collection will be the many side. It is advised to use the Relation annotation to indicate the one side of a relation. Let me give an example

```
@Data
public class Person {
...
  @Relation(opposite="residents")
  private Address address
...
}

@Data
public class Address {
...
  private List<Person> residents;
...
}
```


The annotation should be on the field of the object that should hold the foreign key.

#### one-to-one relationships

One-to-One relations are also supported. Although it always good to ask yourself in case of such relation that you merge the two side into one object, there are sometimes cases where they are useful. Just replace that List with a single class and it will work.

#### many-to-many relationships

Many-to-Many relations are implemented with a relation object just as in a relational database:

```
@Data
public class Person {
...
  private List<Address> addresses;
...
}

@Data
public class Address {
...
  private String type;
  private List<Person> persons;
...
}

@Data
public PersonAddresses {

	@Relation(opposite="addresses")
	private Person person;
	@Relation(opposite="persons")
	private Address address;
}
```

### Inheritance

Bare Bones Persist supports inheritance. Just extend one persistent class from another. The library creates one table for each class and a foreign key over the primary key of the superclass linking the subclass table. The library also adds a subtype field to the parent that it will use as discriminator. Queries of superclasses will retrieve all data of instances of subclasses, create the correct type and return them as rows in a query.

#### Mapped superclasses

If you add the Mapped annotation the a class the library will not create a table for that class. Instead the fields of that class will be inherited in subclasses and added to the table of the subclasses.

# Connect to the database and create (or upgrade) the tables

To start you will need to be able to connect through JDBC to your database. Have the correct user and its schema or database available. The library uses a java.sql.Connection as argument to most of its calls. This means that you are fully in controle to create, open and close it, or use a connection pool for it like HikariCP (https://github.com/brettwooldridge/HikariCP)

The first statement to use is always the call to update, verify or register your persistent objects:

```
    try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost/db", "user",
            "password")) {
      Database database = new Database(() -> new PostgresqlCommands().logger(str -> System.out.println(str)));
      database.transaction(connection, () -> database.upgrade(connection,Person.class,Address.class));
    }
```
The first statement creates the JDBC connection with your url, user and password.

In the second statement you create a database object. The  argument is a Supplier of a CommandScheme. Here you select the subclass of CommandScheme for your database. 

- Oracle: OracleCommands
- MS SQL Server: MsSqlServerCommands
- PostgreSQL: PostgresqlCommands
- MySQL: MySqlCommands
- H2: CommandScheme

You can use the logger method to add a Consumer that can handle logging statements. You may use any of your favorite logging frameworks like log4j or slf4j or bones.log for that matter. When you use that statement the library will use the consumer to send the SQL statement it uses.

In the third statement you use the transaction method to start and commit a transaction, or rollback in case of an exception.
In the transaction, the upgrade method creates, alters and/or drops the tables so the end result will handle statements of the indicated persistent classes.

When you do not want to upgrade your schema, you can use the register or verify method instead. Both these calls need to be called only once within a process and before any other calls to any instance of Database.

Database instances are thread safe, but in general cheap to create because they contain little data. CommandScheme objects are not threadsafe and hold PreparedStatements. So in server applications reuse connections with connectionpools. If you want to close a connection the data it is stored with the connection will be automatically cleared when the connection is garbage collected, with the help of weak references.

# Data manipulation

You can insert, update and delete data with the methods of the same name.

```
    try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost/db", "user",
            "password")) {
...            
      database.transaction(connection, () -> {
        Person person = new Person();
        person.setName("Homer");
        person = database.insert(connection,person);
        person.setName("Bart");
        person = database.update(connection,person);
        database.delete(connection,person);
    }
```

In case of generated primary key fields, the fields will be updated with the generated value.

As you see the insert and update methods return an instance that may differ from the parameter. This will only occur when the object is already available in the transaction.

As stated when an object has a version field and the instance is updated or deleted by another session the library will throw a StaleObjectException; The programmer should refresh the object and present it to the user to review the proposed change.

A relation will be set when a related object is available in the member that forms the foreign key, otherwise the foreign key field(s) will be set to NULL. The many side of a relation is ignored. Any related objects should be inserted explicitly.

# Data retrieval

## Refreshing single objects

The database API has a refresh method that will bring an object in sync with the data stored in the database. The refresh method looks at the values of the primary key fields and selects all other data of this object including its version field if any. It will also look at cached objects and return the one instance with the provided key. If the provided object is of the wrong type in a hierarchy it will return the right type.

## Queries

The Query class is used to select more and more diverse data. It supports single table selects, related rows and aggregate functions like count and sum all in one query. When using the selectJoin method, related objects will have that relation set. It is meant to give you almost as much flexibility as SQL.

### A simple query to select all objects of a table

Let's start with an example

```
try (Query<Person> query =
            database.newQuery(connection, Person.class).orderBy("Person.oid")) {
	for (Person person = query.next(); person != null; person = query.next()) {
	  System.out.println(person);
	}
}
```
As you can see, you create a query with the newQuery method of the database object. You provide as arguments the connection and the class that will hold the result of the query. This can be a persistent class, a simple class from java.lang to receive results of expressions or also a record class with multiple fields. (https://docs.oracle.com/en/java/javase/17/language/records.html)

In this example I use an orderBy statement. This not always necessary, unless you use MS SQL Server. This dbms requires an order by clause if you want to use paging which is obligatory in the library. For other databases it is also good practice, so you know you will always retrieve the objects in the same order.

A Query is a Closeable object, so it is advisable to use a try with resources to close the Query when you are done.

A query will create a PreparedStatement to execute the query. It holds a cache of those PreparedStatements to increase performance when possible. 

It is not advisable to create the where clause on basis of input of user, because that can create security a possibility of SQL injection. Always use questionmarks and separate parameters for user input.

### Implicit joins

```
  database.transaction(connection, () -> {
    try (Query<Person> query = database.newQuery(connection, Person.class)
        .where("#Person.residence.address.houseNumber = ? or #Person.residence|Apartment.floor = ?", 2, 1).orderBy("Person.oid")) {
	    for (Person person = query.next(); person != null; person = query.next()) {
	      person.setResidence(database.refresh(connection, person.getResidence()));
	      house.setAddress(database.refresh(connection, house.getAddress()));
	      System.out.println(person);
	    }
	 }
  });
```

A more advance examples shows some other features. You can use implicite joins with . navigations of roles of relations. Both roles of a relation can be used.

In the implicite joins in the "#Person.residence|Apartment.floor" example you see that floor is a field of the Apartment class that is a subclass of House that is the type of the role residence of Person. Any reference to a field with or with an implicit join should start with a hash '#'.

Parameters in queries use JDBC style questionmarks to indicate its location. The value of the JDBC parameter is given as separate parameter to the where method. The order the parameters should follow the order of the questionmarks in the where clause.

You see the calls to refresh to get the field values of the related objects.

### retrieving joined object

```
  database.transaction(connection, () -> {
    try (Query<Person> query = database.newQuery(connection, Person.class).selectJoin("#Person.residence","house")
          .selectJoin("#house.address","address").orderBy("Person.name, address.zipcode")) {
      for (Person person = query.next(); person != null; person = query.next()) {
        House house = person.getResidence();
        Address address = person.getAddress();
        System.out.println(person);
        System.out.println(house);
        System.out.println(address);
      }
   }
  });
```

By using the selectJoin method you will specify the join and retrieve the joined objects in one call. So if you retrieve the person object, you can get his residence and address by calling the right getters
This mechanism will only work when the joins run from the child to a parent. In other words when the relation in the left object is a single object pointing to the object on the right side of the path.


### Return simple objects

It is possible to return simple values in queries. Look at this simple example:

```
  database.transaction(connection, () -> {
    try (Query<Integer> query = database.newQuery(connection, Integer.class)
        .select("count(*)").from(V2.Person.class).orderBy("result")) {
      Integer count = query.next(); 
      System.out.println(count);
    }
```

In this case you define the select clause explicitly. When you use a single field, the name of the field will always be 'result' as you can see in the orderBy parameter.

### Return multiple object at the same time

You can use a record class to select multiple data items at the same time. Let us look at two examples:

```
public record CountPerLabel(String name, int count) {}

try (Query<CountPerLabel> query = database.newQuery(connection, CountPerLabel.class)
    .select("name", "count(*)").from(Person.class).groupBy("name").orderBy("name")) {
  for (CountPerLabel count = query.next(); count != null; count = query.next()) {
    System.out.println(count);
  }
}
```

With the explicit select method you can select any expression you want. In this case just the name and count(*) from Person. The CountPerLabel record will hold the data per row returned by the query.
The name of the columns are the field names of the record. So if the first fieldname of CountPerLabel was label, you should use "label" as parameter to the orderBy.



