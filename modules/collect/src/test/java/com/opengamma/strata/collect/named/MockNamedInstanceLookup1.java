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
public class MockNamedInstanceLookup1 {

  public static final MockNamed ANOTHER1 = new MockNamed() {
    @Override
    public String getName() {
      return "Another1";
    }
  };

  // package scoped
  static final NamedLookup<MockNamed> INSTANCE = new NamedLookup<MockNamed>() {
    @Override
    public ImmutableMap<String, MockNamed> lookupAll() {
      return ImmutableMap.of("Another1", ANOTHER1);
    }
  };

}
