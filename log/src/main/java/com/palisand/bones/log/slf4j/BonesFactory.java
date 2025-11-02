package com.palisand.bones.log.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class BonesFactory implements ILoggerFactory {

  @Override
  public Logger getLogger(String name) {
    return new BonesLogger(name);
  }

}
