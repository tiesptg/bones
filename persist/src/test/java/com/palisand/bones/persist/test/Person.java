package com.palisand.bones.persist.test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.persist.Database.Index;
import com.palisand.bones.persist.Database.Relation;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Person extends Table {
  
  @Index("name")
  private String name;
  private double wealth;
  private LocalDate birthday;
  private Integer children;
  
  @Relation(opposite="friends")
  private List<Person> friends = new ArrayList<>();
  
  @Relation(opposite="residents")
  private House residence;

}
