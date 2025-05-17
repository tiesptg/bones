package com.palisand.bones.persist.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.palisand.bones.persist.CommandScheme;
import com.palisand.bones.persist.Database;
import com.palisand.bones.persist.OracleCommands;
import com.palisand.bones.persist.PostgresqlCommands;
import com.palisand.bones.persist.Query;
import com.palisand.bones.persist.StaleObjectException;

class UpgradeTest {
  private DB type = DB.ORA;

  public enum DB {
    H2, PG, ORA
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
      case ORA:
        return new Database(() -> new OracleCommands() // .indexForFkNeeded(false)
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
      assertFalse(database.verify(connection, V1.Person.class, V1.House.class, V1.Apartment.class,
          V1.Street.class, V1.Friendship.class));
      database.upgrade(connection, V1.Person.class, V1.House.class, V1.Apartment.class,
          V1.Street.class, V1.Friendship.class);
      database.commit(connection);
      System.out.println("after create");
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
      System.out.println("after upgrade");
      assertTrue(database.verify(connection, V2.Person.class, V2.House.class, V2.Apartment.class,
          V2.Address.class, V2.Friendship.class));
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
            .where("friends.one", "=", list.get(5)).or("friends.other", "=", list.get(5)).execute();
        for (V2.Friendship f = friends.next(); f != null; f = friends.next()) {
          System.out.println(database.refresh(connection, f.getOne()));
          System.out.println(database.refresh(connection, f.getOther()));
          System.out.println();
        }

        friends = database.newQuery(connection, V2.Friendship.class).execute();
        while (!friends.isLastPage()) {
          for (V2.Friendship f = friends.next(); f != null; f = friends.next()) {
            database.delete(connection, f);
          }
          friends.nextPage();
        }
      });

      System.out.println();
      System.out.println("select, update Person");
      V2.Person p3 = (V2.Person) database.transactionWithResult(connection, () -> {
        Query<V2.Person> query = database.newQuery(connection, V2.Person.class);
        query.setRowsPerPage(4);
        V2.Person last = null;
        for (query.execute(); !query.isLastPage(); query.nextPage()) {
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
        p2 = (V2.Person) database.update(connection, p2);
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
            .where("Person.birthday", "=", LocalDate.of(2000, 3, 3)).execute();
        for (V2.Person person = query.next(); person != null; person = query.next()) {
          System.out.println(person);
        }
      });

      System.out.println();
      System.out.println("select and delete all");
      database.transaction(connection, () -> {
        Query<V2.Person> query = database.newQuery(connection, V2.Person.class);
        query.setRowsPerPage(4);
        for (query.execute(); !query.isLastPage(); query.execute()) {
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
          address.setNumber(i);
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
          person.setResidence(house);
          person = database.insert(connection, person);
        }
      });

      System.out.println();
      System.out.println("select person, house & address with number 2");
      database.transaction(connection, () -> {
        Query<V2.Person> query =
            database.newQuery(connection, V2.Person.class).join("Person.residence")
                .join("House.address").where("Address.number", "=", 2).execute();
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
            .where("Person.residence.address.number", "=", 1).execute();
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
            .where("House.residents.children", "=", 1).execute();
        for (V2.House house = query.next(); house != null; house = query.next()) {
          System.out.println(house);
        }
      });
      System.out.println();
      System.out.println("select all and remove");
      database.transaction(connection, () -> {
        Query<V2.Person> query = database.newQuery(connection, V2.Person.class);
        while (!query.isLastPage()) {
          query.execute();
          for (V2.Person person = query.next(); person != null; person = query.next()) {
            V2.House house = database.refresh(connection, person.getResidence());
            person.setResidence(house);
            database.delete(connection, person);
            if (house != null) {
              V2.Address address = null;
              if (house.getAddress() != null) {
                address = database.refresh(connection, house.getAddress());
                house.setAddress(address);
              }
              database.delete(connection, house);
              if (address != null) {
                database.delete(connection, address);
              }
            }
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
            .from(V2.Person.class).execute();
        for (Integer count = query.next(); count != null; count = query.next()) {
          System.out.println(count);
        }
      });

      database.transaction(connection, () -> {
        Query<CountPerLabel> query = database.newQuery(connection, CountPerLabel.class)
            .select("name", "count(*)").from(V2.Person.class).groupBy("name").execute();
        for (CountPerLabel count = query.next(); count != null; count = query.next()) {
          System.out.println(count);
        }
      });

      System.out.println();
      System.out.println("select friends");
      database.transaction(connection, () -> {

        int count = 1;
        while (count != 0) {
          Query<V2.Friendship> friends = database.newQuery(connection, V2.Friendship.class);
          friends.setRowsPerPage(100);
          friends.execute();
          count = 0;
          for (V2.Friendship f = friends.next(); f != null; f = friends.next()) {
            database.delete(connection, f);
            ++count;
          }
        }

        List<V2.Person> all = database.newQuery(connection, V2.Person.class).toList();
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
          address.setNumber((i + 1) * 2);
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
        Query<V2.House> query =
            database.newQuery(connection, V2.House.class).where("house.address", "=", a).execute();
        for (V2.House house = query.next(); house != null; house = query.next()) {
          house.setAddress(database.refresh(connection, house.getAddress()));
          System.out.println(house);
        }
      });
      System.out.println();
      System.out.println("select and delete all");
      database.transaction(connection, () -> {
        Query<V2.House> query = database.newQuery(connection, V2.House.class);
        query.setRowsPerPage(4);
        for (query.execute(); !query.isLastPage(); query.execute()) {
          for (V2.House house = query.next(); house != null; house = query.next()) {
            Query<V2.Person> q2 = database.newQuery(connection, V2.Person.class);
            q2.where("person.residence", "=", house).execute();
            for (V2.Person person = q2.next(); person != null; person = q2.next()) {
              database.delete(connection, person);
            }

            V2.Address address = house.getAddress();
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
          b.setNumber(i);
          database.insert(connection, b);
        }
        for (int i = 0; i < rows; ++i) {
          V3.C b = new V3.C();
          b.setName("C" + i);
          b.setNumber(i);
          b.setDescription("Description = " + i);
          b.setAmount(i + .987);
          database.insert(connection, b);
        }
        for (int i = 0; i < rows; ++i) {
          V3.D b = new V3.D();
          b.setName("D" + i);
          b.setNumber(i);
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
      Query<V3.B> queryb = database.newQuery(connection, V3.B.class);
      for (queryb.execute(); !queryb.isLastPage(); queryb.nextPage()) {
        for (V3.B b = queryb.next(); b != null; b = queryb.next()) {
          System.out.println(b);
        }
      }
      System.out.println();
      System.out.println("select all a's and remove them");
      Query<V3.A> query = database.newQuery(connection, V3.A.class).orderBy("A.oid desc");
      while (!query.isLastPage()) {
        query.execute();
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
