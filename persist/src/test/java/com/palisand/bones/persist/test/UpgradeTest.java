package com.palisand.bones.persist.test;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.jupiter.api.Test;

import com.palisand.bones.persist.Database;
import com.palisand.bones.persist.PostgresqlCommands;

class UpgradeTest {

  @Test
  void upgradeTest() throws Exception {
    Class.forName("org.postgresql.Driver");
    try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost/persisttest","ties","ties")) {
      connection.setAutoCommit(false);
      Database database = new Database(() -> new PostgresqlCommands());
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      database.dropAll(connection);
      database.commit(connection);
      System.out.println("after dropAll");
      database.upgrade(connection,Version1.Person.class,
          Version1.House.class,Version1.Apartment.class,Version1.Street.class);
      database.commit(connection);
      System.out.println("after create");
      database.upgrade(connection,Version2.Person.class,
          Version2.House.class,Version2.Apartment.class,Version2.Address.class);
      database.commit(connection);
      System.out.println("after upgrade");
      database.upgrade(connection,Version2.Person.class,
          Version2.House.class,Version2.Apartment.class);
      database.commit(connection);
      System.out.println("after upgrade without changes");
    }
  }

}
