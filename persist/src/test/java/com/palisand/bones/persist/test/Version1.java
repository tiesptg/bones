package com.palisand.bones.persist.test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.persist.Database.Id;
import com.palisand.bones.persist.Database.Index;
import com.palisand.bones.persist.Database.Mapped;
import com.palisand.bones.persist.Database.Relation;
import com.palisand.bones.persist.Database.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class Version1 {

  @Data
  @Mapped
  public static class Table {

    @Id(generated = true)
    private long oid;

  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class House extends Table {

    private String address;
    private Street street;
    private double someField;

    private List<Person> residents = new ArrayList<>();
  }
  
  @Data
  @EqualsAndHashCode(callSuper=true)
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
    private String name;
    private double wealth;
    private LocalDate birthday;
    private Integer children;

    @Relation(opposite = "friends2")
    private List<Person> friends1 = new ArrayList<>();
    private List<Person> friends2 = new ArrayList<>();

    @Relation(opposite = "residents")
    private House residence;

  }

}
