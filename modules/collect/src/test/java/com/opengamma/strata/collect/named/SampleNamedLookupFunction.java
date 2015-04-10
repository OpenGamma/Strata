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
class SampleNamedLookupFunction implements NamedLookup<SampleNamed> {

  SampleNamedLookupFunction() {
  }

  @Override
  public ImmutableMap<String, SampleNamed> lookupAll() {
    return ImmutableMap.of("Other", OtherSampleNameds.OTHER);
  }

}
