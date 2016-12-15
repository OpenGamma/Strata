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
public class SampleNamedInstanceLookup2 {

  public static final SampleNamed ANOTHER2 = new SampleNamed() {
    @Override
    public String getName() {
      return "Another2";
    }
  };

  // public scoped
  public static final NamedLookup<SampleNamed> INSTANCE = new NamedLookup<SampleNamed>() {
    @Override
    public ImmutableMap<String, SampleNamed> lookupAll() {
      return ImmutableMap.of("Another2", ANOTHER2, "ANOTHER2", ANOTHER2);
    }
  };

}
