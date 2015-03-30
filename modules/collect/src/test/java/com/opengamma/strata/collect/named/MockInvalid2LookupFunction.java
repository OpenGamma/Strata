/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import com.google.common.collect.ImmutableMap;

/**
 * Mock named object function.
 */
class MockInvalid2LookupFunction implements NamedLookup<MockInvalid2> {

  MockInvalid2LookupFunction(String badConstrucor) {
  }

  @Override
  public ImmutableMap<String, MockInvalid2> lookupAll() {
    return ImmutableMap.of();
  }

}
