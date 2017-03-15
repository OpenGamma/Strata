/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
public class SampleInvalid6 implements Named {

  /**
   * Non-static - Error.
   */
  NamedLookup<SampleInvalid6> INSTANCE = null;

  @Override
  public String getName() {
    return null;
  }

}
