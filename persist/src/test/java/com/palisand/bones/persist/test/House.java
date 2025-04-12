package com.palisand.bones.persist.test;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class House extends Table {

  private String address;
  
  private List<Person> residents = new ArrayList<>();
}
