package com.palisand.bones.persist.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.palisand.bones.persist.Database.Db;
import com.palisand.bones.persist.Database.Id;
import com.palisand.bones.persist.Database.Index;
import com.palisand.bones.persist.Database.Mapped;
import com.palisand.bones.persist.Database.Relation;
import com.palisand.bones.persist.Database.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public class V2 {

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
    private List<Person> residents = null;

  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class Address extends Table {
    private String street;
    private int houseNumber;
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

  @Data
  public static class TypeTestWithLobs extends TypeTest {
    private Clob clobField;
    private Blob blobField;
  }

  @Data
  public static class TypeTest {
    @Id(generated = true)
    private long id;
    @Id
    private OffsetDateTime dateField;
    private String stringField;
    private short shortField;
    private int intField;
    private Integer intObjectField;
    private boolean booleanField;
    private byte[] bytesField;
    private BigDecimal decimalField;
    private BigInteger integerField;
    private LocalDate localDateField;
    private LocalDateTime localDateTimeField;
    private Time timeField;
    private Date sqlDateField;
    private UUID uuidField;
  }

}
