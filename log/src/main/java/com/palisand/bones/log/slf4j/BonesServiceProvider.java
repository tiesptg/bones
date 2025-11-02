package com.palisand.bones.log.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class BonesServiceProvider implements SLF4JServiceProvider {
  private static final BonesFactory factory = new BonesFactory();

  @Override
  public ILoggerFactory getLoggerFactory() {
    return factory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return null;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return new NOPMDCAdapter();
  }

  @Override
  public String getRequestedApiVersion() {
    return "2.0.17";
  }

  @Override
  public void initialize() {}

}
