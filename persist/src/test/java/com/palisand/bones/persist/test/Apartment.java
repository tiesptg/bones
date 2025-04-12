package com.palisand.bones.persist.test;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Apartment extends House {

  private int floor;
  private boolean liftAvailable;
}
