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
public class MockNamedInstanceLookup2 {

  public static final MockNamed ANOTHER2 = new MockNamed() {
    @Override
    public String getName() {
      return "Another2";
    }
  };

  // public scoped
  public static final NamedLookup<MockNamed> INSTANCE = new NamedLookup<MockNamed>() {
    @Override
    public ImmutableMap<String, MockNamed> lookupAll() {
      return ImmutableMap.of("Another2", ANOTHER2);
    }
  };

}
