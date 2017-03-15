/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import com.google.common.collect.ImmutableMap;

/**
 * Mock named object function.
 */
class SampleInvalid2LookupFunction implements NamedLookup<SampleInvalid2> {

  SampleInvalid2LookupFunction(String badConstrucor) {
  }

  @Override
  public ImmutableMap<String, SampleInvalid2> lookupAll() {
    return ImmutableMap.of();
  }

}
