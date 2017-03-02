/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
class MoreSampleNameds implements SampleNamed {

  public static final MoreSampleNameds MORE = new MoreSampleNameds();
  public static final String TEXT = "Not a constant";
  static final MoreSampleNameds NOT_PUBLIC = null;
  public final MoreSampleNameds NOT_STATIC = null;
  public static MoreSampleNameds NOT_FINAL = null;

  @Override
  public String getName() {
    return "More";
  }

}
