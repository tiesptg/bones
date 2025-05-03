package com.palisand.bones.persist.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.palisand.bones.persist.CommandScheme;
import com.palisand.bones.persist.Database;
import com.palisand.bones.persist.PostgresqlCommands;
import com.palisand.bones.persist.Query;
import com.palisand.bones.persist.StaleObjectException;

class UpgradeTest {
  private DB type = DB.H2;

  public enum DB {
    H2, PG
  }

  @BeforeAll
  static void init() {
    // type DB.PG
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
    }
    return null;
  }

  Database newDatabase() {
    switch (type) {
      case PG:
        return new Database(() -> new PostgresqlCommands().logger(str -> System.out.println(str)));
      case H2:
        return new Database(() -> new CommandScheme() // .indexForFkNeeded(false)
            .logger(str -> System.out.println(str)));
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
      System.out.println("after dropAll");
      assertFalse(database.verify(connection, Version1.Person.class, Version1.House.class,
          Version1.Apartment.class, Version1.Street.class));
      database.upgrade(connection, Version1.Person.class, Version1.House.class,
          Version1.Apartment.class, Version1.Street.class);
      database.commit(connection);
      System.out.println("after create");
      assertTrue(database.verify(connection, Version1.Person.class, Version1.House.class,
          Version1.Apartment.class, Version1.Street.class));
      Version1.Person person1 = new Version1.Person();
      person1.setName("Ties");
      person1.setBirthday(LocalDate.of(1966, 7, 8));
      person1.setChildren(1);
      person1.setWealth(200111.75);

      Version1.Apartment apartment1 = new Version1.Apartment();
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
      Version1.Person person2 = new Version1.Person();
      person2.setOid(person1.getOid());
      person2 = database.refresh(connection, person2);
      System.out.println(person2);
      database.commit(connection);
      database.upgrade(connection, Version2.Person.class, Version2.House.class,
          Version2.Apartment.class, Version2.Address.class);
      database.commit(connection);
      System.out.println("after upgrade");
      assertTrue(database.verify(connection, Version2.Person.class, Version2.House.class,
          Version2.Apartment.class, Version2.Address.class));
      database.commit(connection);
      System.out.println("after upgrade without changes");
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
      if (!database.verify(connection, Version2.Person.class, Version2.House.class,
          Version2.Apartment.class, Version2.Address.class)) {
        database.upgrade(connection, Version2.Person.class, Version2.House.class,
            Version2.Apartment.class, Version2.Address.class);
      }
      System.out.println();
      System.out.println("add 10 persons");
      for (int i = 0; i < 10; ++i) {
        Version2.Person person = new Version2.Person();
        person.setName("Person" + i);
        person.setBirthday(LocalDate.of(2000, i + 1, i + 1));
        person.setChildren(i);
        person.setWealth(99.09);
        database.insert(connection, person);
      }
      database.commit(connection);
      System.out.println();
      System.out.println("select, update Person");
      Version2.Person p3 = (Version2.Person) database.transactionWithResult(connection, () -> {
        Query<Version2.Person> query = database.newQuery(connection, Version2.Person.class);
        query.setRowsPerPage(4);
        Version2.Person last = null;
        for (query.execute(); !query.isLastPage(); query.nextPage()) {
          for (Version2.Person person = query.next(); person != null; person = query.next()) {
            System.out.println(person);
            last = person;
          }
        }
        Version2.Person p2 = new Version2.Person();
        p2.setOid(last.getOid());
        p2 = database.refresh(connection, p2);
        assertTrue(last == p2);
        p2 = new Version2.Person();
        p2.setOid(last.getOid());
        p2.setOversion(last.getOversion());
        p2.setName("Other name");
        p2.setChildren(4);
        p2.setWealth(55.555);
        p2.setBirthday(LocalDate.of(1977, 8, 9));
        p2 = (Version2.Person) database.update(connection, p2);
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
        Query<Version2.Person> query = database.newQuery(connection, Version2.Person.class)
            .where("Person.birthday", "=", LocalDate.of(2000, 3, 3)).execute();
        for (Version2.Person person = query.next(); person != null; person = query.next()) {
          System.out.println(person);
        }
      });

      System.out.println();
      System.out.println("select and delete all");
      database.transaction(connection, () -> {
        Query<Version2.Person> query = database.newQuery(connection, Version2.Person.class);
        query.setRowsPerPage(4);
        for (query.execute(); !query.isLastPage(); query.execute()) {
          for (Version2.Person person = query.next(); person != null; person = query.next()) {
            database.delete(connection, person);
          }
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
      if (!database.verify(connection, Version2.Person.class, Version2.House.class,
          Version2.Apartment.class, Version2.Address.class)) {
        database.upgrade(connection, Version2.Person.class, Version2.House.class,
            Version2.Apartment.class, Version2.Address.class);
      }
      System.out.println();
      System.out.println("add 5 houses with with addresses");
      Version2.Address a = (Version2.Address) database.transactionWithResult(connection, () -> {
        Version2.Address address = null;
        for (int i = 0; i < 5; ++i) {
          address = new Version2.Address();
          address.setNumber((i + 1) * 2);
          address.setStreet("Dorpsstraat");
          address.setTown("Ons Dorp");

          address = database.insert(connection, address);
          Version2.House house = new Version2.House();
          house.setAddress(address);
          house = database.insert(connection, house);
        }
        return address;
      });
      System.out.println();
      System.out.println("select with simple join");
      database.transaction(connection, () -> {
        Query<Version2.House> query = database.newQuery(connection, Version2.House.class)
            .where("house.address", "=", a).execute();
        for (Version2.House house = query.next(); house != null; house = query.next()) {
          house.setAddress(database.refresh(connection, house.getAddress()));
          System.out.println(house);
        }
      });
      System.out.println();
      System.out.println("select and delete all");
      database.transaction(connection, () -> {
        Query<Version2.House> query = database.newQuery(connection, Version2.House.class);
        query.setRowsPerPage(4);
        for (query.execute(); !query.isLastPage(); query.execute()) {
          for (Version2.House house = query.next(); house != null; house = query.next()) {
            Query<Version2.Person> q2 = database.newQuery(connection, Version2.Person.class);
            q2.where("person.residence", "=", house).execute();
            for (Version2.Person person = q2.next(); person != null; person = q2.next()) {
              database.delete(connection, person);
            }

            Version2.Address address = house.getAddress();
            database.delete(connection, house);
            if (address != null) {
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

}
