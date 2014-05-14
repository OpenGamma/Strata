/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalIdBundle;

/**
 * An immutable LiveDataResults implementation. No mutator methods
 * are available and the objects held are all immutable.
 */
public class DefaultImmutableLiveDataResults implements ImmutableLiveDataResults {

  /**
   * Convenience static field for returning an
   * empty ImmutableLiveDataResults.
   */
  public static final ImmutableLiveDataResults EMPTY =
      new DefaultImmutableLiveDataResults(ImmutableMap.<ExternalIdBundle, LiveDataResult>of());

  /**
   * The market data results.
   */
  private final ImmutableMap<ExternalIdBundle, LiveDataResult> _results;

  /**
   * Create a new instance populating it with the supplied
   * market data results.
   *
   * @param results  the results to populate the mapper with, not null
   */
  public DefaultImmutableLiveDataResults(Map<ExternalIdBundle, LiveDataResult> results) {
    // ImmutableMap implementation will take care of
    // non-null field, keys and values
    _results = ImmutableMap.copyOf(results);
  }

  @Override
  public boolean containsTicker(ExternalIdBundle ticker) {
    return _results.containsKey(ticker);
  }

  @Override
  public LiveDataResult get(ExternalIdBundle ticker) {
    return _results.get(ticker);
  }

  @Override
  public boolean isPending(ExternalIdBundle ticker) {
    if (containsTicker(ticker)) {
      return _results.get(ticker).isPending();
    } else {
      throw new IllegalArgumentException("No result found for ticker: " + ticker);
    }
  }

  @Override
  public int size() {
    return _results.size();
  }

}
