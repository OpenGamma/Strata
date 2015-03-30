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
class MockNamedLookupFunction implements NamedLookup<MockNamed> {

  MockNamedLookupFunction() {
  }

  @Override
  public ImmutableMap<String, MockNamed> lookupAll() {
    return ImmutableMap.of("Other", OtherMockNameds.OTHER);
  }

}
