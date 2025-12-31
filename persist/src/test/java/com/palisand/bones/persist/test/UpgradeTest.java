package com.palisand.bones.persist.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.palisand.bones.persist.CommandScheme;
import com.palisand.bones.persist.Database;
import com.palisand.bones.persist.MsSqlServerCommands;
import com.palisand.bones.persist.MySqlCommands;
import com.palisand.bones.persist.OracleCommands;
import com.palisand.bones.persist.PostgresqlCommands;
import com.palisand.bones.persist.Query;
import com.palisand.bones.persist.StaleObjectException;
import com.palisand.bones.persist.test.V2.TypeTest;

class UpgradeTest {
  private DB type = DB.H2;

  public enum DB {
    H2, PG, ORA, MYS, MSSQL
  }

  public boolean supportsLargeObjects() {
    return type != DB.PG;
  }

  Connection getConnection() throws Exception {
    switch (type) {
      case H2: {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:test");
      }
      case PG: {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection("jdbc:postgresql://localhost/persisttest", "ties",
            "ties");
      }
      case ORA:
        Class.forName("oracle.jdbc.driver.OracleDriver");
        return DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521/FREEPDB1", "ties",
            "ties");
      case MYS:
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/persisttest", "ties",
            "ties");
      case MSSQL:
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(
            "jdbc:sqlserver://localhost:1433;trustServerCertificate=true;database=persisttest",
            "ties", "Ti3sTi3s");
    }
    return null;
  }

  Database newDatabase() {
    switch (type) {
      case PG:
        return new Database(() -> new PostgresqlCommands().logger(str -> System.out.println(str)));
      case H2:
        return new Database(() -> new CommandScheme().logger(str -> System.out.println(str)));
      case ORA:
        return new Database(() -> new OracleCommands().logger(str -> System.out.println(str)));
      case MYS:
        return new Database(() -> new MySqlCommands().logger(str -> System.out.println(str)));
      case MSSQL:
        return new Database(() -> new MsSqlServerCommands().logger(str -> System.out.println(str)));
    }
    return new Database(() -> new CommandScheme().logger(str -> System.out.println(str)));
  }

  @Test
  void upgradeTest() throws Exception {
    try (Connection connection = getConnection()) {
      Database database = newDatabase();
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      database.dropAll(connection);
      database.commit(connection);
      assertFalse(database.verify(connection, V1.Person.class, V1.House.class, V1.Apartment.class,
          V1.Street.class, V1.Friendship.class));
      database.upgrade(connection, V1.Person.class, V1.House.class, V1.Apartment.class,
          V1.Street.class, V1.Friendship.class);
      database.commit(connection);
      assertTrue(database.verify(connection, V1.Person.class, V1.House.class, V1.Apartment.class,
          V1.Street.class, V1.Friendship.class));
      V1.Person person1 = new V1.Person();
      person1.setName("Ties");
      person1.setBirthday(LocalDate.of(1966, 7, 8));
      person1.setChildren(1);
      person1.setWealth(200111.75);

      V1.Apartment apartment1 = new V1.Apartment();
      apartment1.setAddress("Ergens");
      apartment1.setFloor(1);
      apartment1.setLiftAvailable(true);
      database.insert(connection, person1);
      database.insert(connection, apartment1);
      database.commit(connection);
      person1.setName("Ties Pull ter Gunne");
      apartment1.setLiftAvailable(false);
      person1.setResidence(apartment1);
      database.update(connection, person1);
      database.update(connection, apartment1);
      database.commit(connection);
      V1.Person person2 = new V1.Person();
      person2.setOid(person1.getOid());
      person2 = database.refresh(connection, person2);
      System.out.println(person2);
      database.commit(connection);
      database.upgrade(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
          V2.Address.class, V2.Friendship.class);
      database.commit(connection);
      assertTrue(database.verify(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
          V2.Address.class, V2.Friendship.class));
      database.commit(connection);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  @Test
  void testTypes() {
    String data =
        "This is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of data"
            + "This is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of data"
            + "This is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of data"
            + "This is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of data"
            + "This is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of data"
            + "This is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of data"
            + "This is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of dataThis is a lot of data";
    try (Connection connection = getConnection()) {
      Database database = newDatabase();
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      database.dropAll(connection);
      if (supportsLargeObjects()) {
        database.upgrade(connection, V2.TypeTest.class, V2.TypeTestWithLobs.class);
      } else {
        database.upgrade(connection, V2.TypeTest.class);
      }
      System.out.println();
      System.out.println("Add TypeTest");
      database.transaction(connection, () -> {
        TypeTest object = supportsLargeObjects() ? new V2.TypeTestWithLobs() : new V2.TypeTest();
        if (object instanceof V2.TypeTestWithLobs tt) {
          Blob blob = connection.createBlob();
          blob.setBytes(1, data.getBytes());
          tt.setBlobField(blob);
          Clob clob = connection.createClob();
          clob.setString(1, data);
          tt.setClobField(clob);
        }
        object.setBytesField(data.getBytes());
        object.setBooleanField(true);
        object.setDateField(OffsetDateTime.now());
        object.setDecimalField(new BigDecimal("1234567.89"));
        object.setIntegerField(new BigInteger("8765"));
        object.setIntField(93939393);
        object.setIntObjectField(88776655);
        object.setLocalDateField(LocalDate.now());
        object.setLocalDateTimeField(LocalDateTime.now());
        object.setShortField((short) 8866);
        object.setStringField("This is just a short field");
        object.setTimeField(Time.valueOf(LocalTime.now()));
        object.setSqlDateField(new Date(new java.util.Date().getTime()));
        object.setUuidField(UUID.randomUUID());
        System.out.println(object);
        database.insert(connection, object);
        try (Query<TypeTest> query = database.newQuery(connection, TypeTest.class)
            .orderBy("TypeTest.id, TypeTest.shortField")) {
          object = query.next();
          assertNotNull(object);
          System.out.println(object);
          database.delete(connection, object);
        }
      });
    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  @Test
  void testQuery() {
    try (Connection connection = getConnection()) {
      Database database = newDatabase();
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      if (!database.verify(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
          V2.Address.class, V2.Friendship.class)) {
        database.upgrade(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
            V2.Address.class, V2.Friendship.class);
      }
      System.out.println();
      System.out.println("add 10 persons");
      int amount = 10;
      List<V2.Person> list = new ArrayList<>();
      for (int i = 0; i < amount; ++i) {
        V2.Person person = new V2.Person();
        person.setName("Person" + i);
        person.setBirthday(LocalDate.of(2000, i + 1, i + 1));
        person.setChildren(i);
        person.setWealth(99.09);
        list.add(database.insert(connection, person));
      }
      for (int i = 0; i < amount - 5; i++) {
        for (int j = i + 1; j < i + 5; ++j) {
          V2.Friendship f = new V2.Friendship();
          f.setOne(list.get(i));
          f.setOther(list.get(j));
          database.insert(connection, f);
        }
      }
      database.commit(connection);
      System.out.println();
      System.out.println("select friends");
      database.transaction(connection, () -> {
        Query<V2.Friendship> friends = database.newQuery(connection, V2.Friendship.class, "friends")
            .where("#friends.one = ? or #friends.other = ?", list.get(5), list.get(5))
            .orderBy("friends.one_oid,friends.other_oid");
        for (V2.Friendship f = friends.next(); f != null; f = friends.next()) {
          System.out.println(database.refresh(connection, f.getOne()));
          System.out.println(database.refresh(connection, f.getOther()));
          System.out.println();
        }

        friends = database.newQuery(connection, V2.Friendship.class)
            .orderBy("Friendship.one_oid,Friendship.other_oid");
        while (!friends.isLastPage()) {
          for (V2.Friendship f = friends.next(); f != null; f = friends.next()) {
            database.delete(connection, f);
          }
          friends.firstPage();
        }
      });

      System.out.println();
      System.out.println("select, update Person");
      V2.Person p3 = (V2.Person) database.transactionWithResult(connection, () -> {
        Query<V2.Person> query =
            database.newQuery(connection, V2.Person.class).orderBy("Person.oid").rowsPerPage(4);
        V2.Person last = null;
        for (; !query.isLastPage(); query.nextPage()) {
          for (V2.Person person = query.next(); person != null; person = query.next()) {
            System.out.println(person);
            last = person;
          }
        }
        V2.Person p2 = new V2.Person();
        p2.setOid(last.getOid());
        p2 = database.refresh(connection, p2);
        assertTrue(last == p2);
        p2 = new V2.Person();
        p2.setOid(last.getOid());
        p2.setOversion(last.getOversion());
        p2.setName("Other name");
        p2.setChildren(4);
        p2.setWealth(55.555);
        p2.setBirthday(LocalDate.of(1977, 8, 9));
        p2 = database.update(connection, p2);
        assertTrue(last == p2);
        System.out.println(p2);
        return p2;
      });
      System.out.println();
      System.out.println("optimistic locking check");
      p3.setOversion(0);
      Exception exception =
          assertThrows(StaleObjectException.class, () -> database.update(connection, p3));
      System.out.println("Expected exception: " + exception);
      database.commit(connection);

      System.out.println();
      System.out.println("refresh & delete");
      database.transaction(connection, () -> {
        database.refresh(connection, p3);
        database.delete(connection, p3);
      });

      System.out.println();
      System.out.println("select with simple where");
      database.transaction(connection, () -> {
        Query<V2.Person> query = database.newQuery(connection, V2.Person.class)
            .where("#Person.birthday = ?", LocalDate.of(2000, 3, 3)).orderBy("Person.oid");
        for (V2.Person person = query.next(); person != null; person = query.next()) {
          System.out.println(person);
        }
      });

      System.out.println();
      System.out.println("select and delete all");
      database.transaction(connection, () -> {
        Query<V2.Person> query =
            database.newQuery(connection, V2.Person.class).orderBy("Person.oid").rowsPerPage(4);
        for (; !query.isLastPage(); query.firstPage()) {
          for (V2.Person person = query.next(); person != null; person = query.next()) {
            database.delete(connection, person);
          }
        }
      });
    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  public record PersonHouseAddress(V2.Person person, V2.House house, V2.Address address) {
  }

  @Test
  void testQueryWithJoin() {
    try (Connection connection = getConnection()) {
      Database database = newDatabase();
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      if (!database.verify(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
          V2.Address.class, V2.Friendship.class)) {
        database.upgrade(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
            V2.Address.class, V2.Friendship.class);
      }
      System.out.println();
      System.out.println("joined objects");
      database.transaction(connection, () -> {
        for (int i = 0; i < 3; ++i) {
          V2.Address address = new V2.Address();
          address.setStreet("Kleine Veer");
          address.setHouseNumber(i);
          address.setTown("Empel");
          address = database.insert(connection, address);

          V2.Apartment house = new V2.Apartment();
          house.setAddress(address);
          house.setFloor(i);
          house.setLiftAvailable(i % 2 == 0);
          house = database.insert(connection, house);

          V2.Person person = new V2.Person();
          person.setName("Ties" + i);
          person.setBirthday(LocalDate.of(1966, 7, i + 4));
          person.setChildren(i);
          person.setWealth(4444.55 + i);
          if (i != 1) {
            person.setResidence(house);
          }
          person = database.insert(connection, person);
        }
      });

      System.out.println();
      System.out.println("select person, house & address with number 2");
      database.transaction(connection, () -> {
        try (Query<V2.Person> query = database.newQuery(connection, V2.Person.class)
            .join("#Person.residence", "house").join("#house.address", "address")
            .where("#address.houseNumber = ?", 2).orderBy("Person.oid")) {
          for (V2.Person person = query.next(); person != null; person = query.next()) {
            V2.House house = database.refresh(connection, person.getResidence());
            person.setResidence(house);
            V2.Address address = database.refresh(connection, house.getAddress());
            house.setAddress(address);
            System.out.println(person);
          }
        }
      });
      System.out.println();
      System.out.println("select person, house & address in one query");
      database.transaction(connection, () -> {
        try (Query<PersonHouseAddress> query = database
            .newQuery(connection, PersonHouseAddress.class).join("#person.residence", "house")
            .join("#house.address", "address").orderBy("person.oid")) {
          for (PersonHouseAddress pha = query.next(); pha != null; pha = query.next()) {
            System.out.println(pha);
          }
        }
      });
      System.out.println();
      System.out.println("select person, house & address in one query with select joins");
      database.transaction(connection, () -> {
        try (Query<V2.Person> query = database.newQuery(connection, V2.Person.class, "person")
            .selectJoin("#person.residence", V2.House.class, "house")
            .selectJoin("#house.address", V2.Address.class, "address").orderBy("person.oid")) {
          for (V2.Person pha = query.next(); pha != null; pha = query.next()) {
            System.out.println(pha);
          }
        }
      });
      System.out.println();
      System.out.println("select person, house & address with number 2 or apartment on floor 4");
      database.transaction(connection, () -> {
        Query<V2.Person> query = database.newQuery(connection, V2.Person.class).where(
            "#Person.residence.address.houseNumber = ? or #Person.residence|Apartment.floor = ?", 2,
            1).orderBy("Person.oid");
        for (V2.Person person = query.next(); person != null; person = query.next()) {
          V2.House house = database.refresh(connection, person.getResidence());
          person.setResidence(house);
          V2.Address address = database.refresh(connection, house.getAddress());
          house.setAddress(address);
          System.out.println(person);
        }
      });
      System.out.println();
      System.out.println("multiple joins in one statement");
      database.transaction(connection, () -> {
        Query<V2.Person> query = database.newQuery(connection, V2.Person.class)
            .where("#Person.residence.address.houseNumber = ?", 1).orderBy("Person.oid");
        for (V2.Person person = query.next(); person != null; person = query.next()) {
          V2.House house = database.refresh(connection, person.getResidence());
          person.setResidence(house);
          V2.Address address = database.refresh(connection, house.getAddress());
          house.setAddress(address);
          System.out.println(person);
        }
      });
      System.out.println();
      System.out.println("join through opposite");
      database.transaction(connection, () -> {
        Query<V2.House> query = database.newQuery(connection, V2.House.class)
            .where("#House.residents.children=?", 1).orderBy("House.oid");
        for (V2.House house = query.next(); house != null; house = query.next()) {
          System.out.println(house);
        }
      });
      System.out.println();
      System.out.println("select all and remove");
      database.transaction(connection, () -> {
        Query<V2.Person> query =
            database.newQuery(connection, V2.Person.class).orderBy("Person.oid");
        while (!query.isLastPage()) {
          for (V2.Person person = query.next(); person != null; person = query.next()) {
            database.delete(connection, person);
          }
          if (!query.nextPage()) {
            break;
          }
        }
        Query<V2.House> query2 = database.newQuery(connection, V2.House.class).orderBy("House.oid");
        while (!query2.isLastPage()) {
          for (V2.House house = query2.next(); house != null; house = query2.next()) {
            database.delete(connection, house);
          }
          if (!query2.nextPage()) {
            break;
          }
        }
        Query<V2.Address> query3 =
            database.newQuery(connection, V2.Address.class).orderBy("Address.oid");
        while (!query3.isLastPage()) {
          for (V2.Address address = query3.next(); address != null; address = query3.next()) {
            database.delete(connection, address);
          }
          if (!query3.nextPage()) {
            break;
          }
        }
      });
    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  public record CountPerLabel(String label, int count) {
  }

  public record Friends(V2.Person one, V2.Person other) {
  }

  @Test
  void testQueryMisc() {
    try (Connection connection = getConnection()) {
      Database database = newDatabase();
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      if (!database.verify(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
          V2.Address.class, V2.Friendship.class)) {
        database.upgrade(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
            V2.Address.class, V2.Friendship.class);
      }
      System.out.println();
      System.out.println("add 10 persons");
      int amount = 10;
      List<V2.Person> list = new ArrayList<>();
      for (int i = 0; i < amount; ++i) {
        V2.Person person = new V2.Person();
        person.setName("Person" + i);
        person.setBirthday(LocalDate.of(2000, i + 1, i + 1));
        person.setChildren(i);
        person.setWealth(99.09);
        list.add(database.insert(connection, person));
      }
      for (int i = 0; i < amount - 5; i++) {
        for (int j = i + 1; j < i + 5; ++j) {
          V2.Friendship f = new V2.Friendship();
          f.setOne(list.get(i));
          f.setOther(list.get(j));
          database.insert(connection, f);
        }
      }
      database.commit(connection);

      database.transaction(connection, () -> {
        Query<Integer> query = database.newQuery(connection, Integer.class).select("count(*)")
            .from(V2.Person.class).orderBy("result");
        for (Integer count = query.next(); count != null; count = query.next()) {
          System.out.println(count);
        }
      });

      database.transaction(connection, () -> {
        try (Query<CountPerLabel> query = database.newQuery(connection, CountPerLabel.class)
            .select("name", "count(*)").from(V2.Person.class).groupBy("name").orderBy("name")) {
          for (CountPerLabel count = query.next(); count != null; count = query.next()) {
            System.out.println(count);
          }
        }
      });

      database.transaction(connection, () -> {
        try (Query<CountPerLabel> query = database.newQuery(connection, CountPerLabel.class)
            .select("name", "count(*)").from(V2.Person.class).groupBy("name").orderBy("name")) {
          for (CountPerLabel count = query.next(); count != null; count = query.next()) {
            System.out.println(count);
          }
        }
      });

      System.out.println();
      System.out.println("select friends in one statement");
      database.transaction(connection, () -> {
        try (Query<Friends> query =
            database.newQuery(connection, Friends.class).from(V2.Friendship.class, "f")
                .join("#f.one", "one").join("#f.other", "other").orderBy("one.oid,other.oid")) {
          for (Friends friends = query.next(); friends != null; friends = query.next()) {
            System.out.println(friends);
          }
        }
      });

      System.out.println();
      System.out.println("select friends");
      database.transaction(connection, () -> {

        int count = 1;
        while (count != 0) {
          Query<V2.Friendship> friends = database.newQuery(connection, V2.Friendship.class)
              .orderBy("Friendship.one_oid,Friendship.other_oid").rowsPerPage(100);
          count = 0;
          for (V2.Friendship f = friends.next(); f != null; f = friends.next()) {
            database.delete(connection, f);
            ++count;
          }
        }

        List<V2.Person> all =
            database.newQuery(connection, V2.Person.class).orderBy("Person.oid").toList();
        for (V2.Person person : all) {
          database.delete(connection, person);
        }
      });
    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  @Test
  void testLink() {
    try (Connection connection = getConnection()) {
      Database database = newDatabase();
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      if (!database.verify(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
          V2.Address.class, V2.Friendship.class)) {
        database.upgrade(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
            V2.Address.class, V2.Friendship.class);
      }
      System.out.println();
      System.out.println("add 5 houses with with addresses");
      V2.Address a = (V2.Address) database.transactionWithResult(connection, () -> {
        V2.Address address = null;
        for (int i = 0; i < 5; ++i) {
          address = new V2.Address();
          address.setHouseNumber((i + 1) * 2);
          address.setStreet("Dorpsstraat");
          address.setTown("Ons Dorp");

          address = database.insert(connection, address);
          V2.House house = new V2.House();
          house.setAddress(address);
          house = database.insert(connection, house);
        }
        return address;
      });
      System.out.println();
      System.out.println("select with simple join");
      database.transaction(connection, () -> {
        Query<V2.House> query = database.newQuery(connection, V2.House.class)
            .where("#House.address = ?", a).orderBy("House.oid");
        for (V2.House house = query.next(); house != null; house = query.next()) {
          house.setAddress(database.refresh(connection, house.getAddress()));
          System.out.println(house);
        }
      });
      System.out.println();
      System.out.println("select with in");
      database.transaction(connection, () -> {
        Query<V2.House> query = database.newQuery(connection, V2.House.class)
            .where("#House.address.houseNumber in (?,?,?)", 4, 6, 8).orderBy("House.oid");
        for (V2.House house = query.next(); house != null; house = query.next()) {
          house.setAddress(database.refresh(connection, house.getAddress()));
          System.out.println(house);
        }
      });
      System.out.println();
      System.out.println("select with between");
      database.transaction(connection, () -> {
        Query<V2.House> query = database.newQuery(connection, V2.House.class)
            .where("#House.address.houseNumber between ? and ?", 1, 5).orderBy("House.oid");
        for (V2.House house = query.next(); house != null; house = query.next()) {
          house.setAddress(database.refresh(connection, house.getAddress()));
          System.out.println(house);
        }
      });
      System.out.println();
      System.out.println("select and delete all");
      database.transaction(connection, () -> {
        Query<V2.House> query =
            database.newQuery(connection, V2.House.class).orderBy("House.oid").rowsPerPage(4);
        for (; !query.isLastPage(); query.firstPage()) {
          for (V2.House house = query.next(); house != null; house = query.next()) {
            Query<V2.Person> q2 = database.newQuery(connection, V2.Person.class);
            V2.House h = house;
            q2.where("#Person.residence = ?", h).orderBy("Person.oid");
            for (V2.Person person = q2.next(); person != null; person = q2.next()) {
              database.delete(connection, person);
            }

            V2.Address address = house.getAddress();
            database.delete(connection, house);
            if (address != null) {
              database.refresh(connection, address);
              database.delete(connection, address);
            }
          }
        }
      });
    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  @Test
  void testHierarchy() {
    try (Connection connection = getConnection()) {
      Database database = newDatabase();
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      if (!database.verify(connection, V3.A.class, V3.B.class, V3.C.class, V3.D.class, V3.E.class,
          V3.M.class)) {
        System.out.println();
        System.out.println("upgrade to V3");
        database.upgrade(connection, V3.A.class, V3.B.class, V3.C.class, V3.D.class, V3.E.class,
            V3.M.class);
      }
      database.transaction(connection, () -> {
        int rows = 5;
        V3.A last = null;
        for (int i = 0; i < rows; ++i) {
          V3.A a = new V3.A();
          a.setName("A" + i);
          database.insert(connection, a);
          last = a;
        }
        for (int i = 0; i < rows; ++i) {
          V3.B b = new V3.B();
          b.setName("B" + i);
          b.setTheNumber(i);
          database.insert(connection, b);
        }
        for (int i = 0; i < rows; ++i) {
          V3.C b = new V3.C();
          b.setName("C" + i);
          b.setTheNumber(i);
          b.setDescription("Description = " + i);
          b.setAmount(i + .987);
          database.insert(connection, b);
        }
        for (int i = 0; i < rows; ++i) {
          V3.D b = new V3.D();
          b.setName("D" + i);
          b.setTheNumber(i);
          b.setDescription("Description = " + i);
          b.setBetter(i % 2 == 0);
          database.insert(connection, b);
        }
        for (int i = 0; i < rows; ++i) {
          V3.E b = new V3.E();
          b.setName("E" + i);
          b.setA(last);
          database.insert(connection, b);
        }
      });
      System.out.println();
      System.out.println("select all b's");
      Query<V3.B> queryb = database.newQuery(connection, V3.B.class).orderBy("B.oid");
      for (queryb.firstPage(); !queryb.isLastPage(); queryb.nextPage()) {
        for (V3.B b = queryb.next(); b != null; b = queryb.next()) {
          System.out.println(b);
        }
      }
      System.out.println();
      System.out.println("select all a's and remove them");
      Query<V3.A> query = database.newQuery(connection, V3.A.class).orderBy("A.oid desc");
      while (!query.isLastPage()) {
        query.firstPage();
        for (V3.A a = query.next(); a != null; a = query.next()) {
          System.out.println(a);
          database.delete(connection, a);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

}
