/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
public class SampleNameds implements SampleNamed {

  public static final SampleNameds STANDARD = new SampleNameds();

  @Override
  public String getName() {
    return "Standard";
  }

}
