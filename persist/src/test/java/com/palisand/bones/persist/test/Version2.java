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
import lombok.ToString;

public class Version2 {

  @Data
  @Mapped
  public static class Table {

    @Id(generated = true)
    private long oid;

    @Version
    private int oversion;

  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class House extends Table {

    private Address address;
    private List<Person> residents = new ArrayList<>();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class Address extends Table {
    private String street;
    private int number;
    private String town;

  }


  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class Apartment extends House {

    private int floor;
    private boolean liftAvailable;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
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
