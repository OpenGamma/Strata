/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
public interface SampleNamed extends Named {

  // for NamedTest
  public static SampleNamed of(String name) {
    if (name.equals("Standard")) {
      return SampleNameds.STANDARD;
    }
    throw new IllegalArgumentException("Name not found");
  }

}
