package com.palisand.bones.persist.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.palisand.bones.persist.CommandScheme;
import com.palisand.bones.persist.Database;
import com.palisand.bones.persist.PostgresqlCommands;

class UpgradeTest {
	private DB type = DB.PG;
	
	public enum DB {
		H2, PG
	}
	
	@BeforeAll
	static void init() {
		//type  DB.PG
	}
	
	Connection getConnection() throws Exception {
		switch (type) {
			case H2: {
				Class.forName("org.h2.Driver");
				return DriverManager.getConnection("jdbc:h2:mem:test");
			}
			case PG: {
		    Class.forName("org.postgresql.Driver");
		    return DriverManager.getConnection("jdbc:postgresql://localhost/persisttest","ties","ties");
			}
		}
		return null;
	}
	
	@SuppressWarnings("incomplete-switch")
	Database newDatabase() {
		switch (type) {
			case PG: return new Database(() -> new PostgresqlCommands().logger(str -> System.out.println(str)));
		}
		return new Database(() -> new CommandScheme().logger(str -> System.out.println(str)));
	}

  @Test
  void upgradeTest() throws Exception {
    try (Connection connection = getConnection()) {
      Database database = new Database(() -> new PostgresqlCommands().logger(str -> System.out.println(str)));
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      database.dropAll(connection);
      database.commit(connection);
      System.out.println("after dropAll");
      database.upgrade(connection,Version1.Person.class,
          Version1.House.class,Version1.Apartment.class,Version1.Street.class);
      database.commit(connection);
      System.out.println("after create");
      database.upgrade(connection,Version1.Person.class,
          Version1.House.class,Version1.Apartment.class,Version1.Street.class);
      database.commit(connection);
      System.out.println("should not show any updates");
      Version1.Person person1 = new Version1.Person();
      person1.setName("Ties");
      person1.setBirthday(LocalDate.of(1966,7,8));
      person1.setChildren(1);
      person1.setWealth(200111.75);
      
      Version1.Apartment apartment1 = new Version1.Apartment();
      apartment1.setAddress("Ergens");
      apartment1.setFloor(1);
      apartment1.setLiftAvailable(true);
      database.insert(connection,person1,apartment1);
      database.commit(connection);
      person1.setName("Ties Pull ter Gunne");
      apartment1.setLiftAvailable(false);
      person1.setResidence(apartment1);
      database.update(connection, person1,apartment1);
      database.commit(connection);
      database.upgrade(connection,Version2.Person.class,
          Version2.House.class,Version2.Apartment.class,Version2.Address.class);
      database.commit(connection);
      System.out.println("after upgrade");
      database.upgrade(connection,Version2.Person.class,
          Version2.House.class,Version2.Apartment.class,Version2.Address.class);
      database.commit(connection);
      System.out.println("after upgrade without changes");
    }
  }

}
