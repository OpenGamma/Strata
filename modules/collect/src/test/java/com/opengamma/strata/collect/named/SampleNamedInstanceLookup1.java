/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import com.google.common.collect.ImmutableMap;

/**
 * Mock named object.
 */
public class SampleNamedInstanceLookup1 {

  public static final SampleNamed ANOTHER1 = new SampleNamed() {
    @Override
    public String getName() {
      return "Another1";
    }
  };

  // package scoped
  static final NamedLookup<SampleNamed> INSTANCE = new NamedLookup<SampleNamed>() {
    @Override
    public ImmutableMap<String, SampleNamed> lookupAll() {
      return ImmutableMap.of("Another1", ANOTHER1, "ANOTHER1", ANOTHER1);
    }
  };

}
