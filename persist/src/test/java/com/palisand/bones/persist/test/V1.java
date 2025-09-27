package com.palisand.bones.persist.test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.palisand.bones.persist.Database.Db;
import com.palisand.bones.persist.Database.Id;
import com.palisand.bones.persist.Database.Index;
import com.palisand.bones.persist.Database.Relation;
import com.palisand.bones.persist.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class V1 {

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class House extends Table {

    private String address;
    private Street street;
    private double someField;

    private List<Person> residents = new ArrayList<>();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Street extends Table {
    private String name;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Apartment extends House {

    private int floor;
    private boolean liftAvailable;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Person extends Table {

    @Index("name")
    @Db(size = 100)
    private String name;
    private double wealth;
    private LocalDate birthday;
    private Integer children;

    private List<Person> friends;

    @Relation(opposite = "residents")
    private House residence;

  }

  @Data
  public static class Friendship {
    @Id
    private Person one;
    @Id
    private Person other;
  }

}
