/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
class OtherSampleNameds implements SampleNamed {

  public static final OtherSampleNameds OTHER = new OtherSampleNameds();

  @Override
  public String getName() {
    return "Other";
  }

}
