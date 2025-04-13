package com.palisand.bones.persist.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

import com.palisand.bones.persist.Database;
import com.palisand.bones.persist.PostgresqlCommands;

public class MainTest {

  public static void main(String[] args) throws Exception {
    Class.forName("org.postgresql.Driver");
    try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost/persisttest","ties","ties")) {
      connection.setAutoCommit(false);
      Database database = new Database(() -> new PostgresqlCommands());
      System.out.println("Connected succesfully to " + database.getDatabaseName(connection));
      database.drop(connection,Person.class,House.class,Apartment.class);
      database.upgrade(connection,Person.class,House.class,Apartment.class);
      Person person = new Person();
      person.setName("Ties P");
      person.setBirthday(LocalDate.of(1966,7,8));
      person.setWealth(400.95);
      database.startTransaction(connection);
      database.insert(connection,person);
      database.commit(connection);
    }
    
  }

}
