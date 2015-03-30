/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Tests for {@link SearchResult}, primarily for test coverage.
 */
@Test
public class SearchResultTest {

  public void coverage() {
    coverImmutableBean(SearchResult.partialMatch(ImmutableSet.of()));
    coverImmutableBean(SearchResult.fullMatch(ImmutableSet.of()));
  }
}
