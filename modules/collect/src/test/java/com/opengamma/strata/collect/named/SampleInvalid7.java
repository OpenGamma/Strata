/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
public class SampleInvalid7 implements Named {

  /**
   * Not-NamedLookup - Error.
   */
  static Object INSTANCE = null;

  @Override
  public String getName() {
    return null;
  }

}
