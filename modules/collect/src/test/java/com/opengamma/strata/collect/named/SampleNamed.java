/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
public interface SampleNamed extends UberNamed {

  // for NamedTest
  public static SampleNamed of(String name) {
    if (name.equals("Standard")) {
      return SampleNameds.STANDARD;
    }
    throw new IllegalArgumentException("Name not found");
  }

  public static ExtendedEnum<SampleNamed> extendedEnum() {
    return ExtendedEnum.of(SampleNamed.class);
  }

}
