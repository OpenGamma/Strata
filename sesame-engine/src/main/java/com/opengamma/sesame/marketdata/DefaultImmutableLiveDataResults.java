/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.id.ExternalIdBundle;

/**
 * An immutable representation of the results of live market data.
 * <p>
 * This class is immutable and thread-safe.
 */
final class DefaultImmutableLiveDataResults implements ImmutableLiveDataResults {

  /**
   * An empty set of results.
   */
  public static final ImmutableLiveDataResults EMPTY =
      new DefaultImmutableLiveDataResults(ImmutableMap.<ExternalIdBundle, LiveDataResult>of());

  /**
   * The market data results.
   */
  private final ImmutableMap<ExternalIdBundle, LiveDataResult> _results;

  /**
   * Create a new instance populating it with the supplied market data results.
   *
   * @param results  the results to populate the mapper with, not null
   */
  public DefaultImmutableLiveDataResults(Map<ExternalIdBundle, LiveDataResult> results) {
    // ImmutableMap implementation will take care of
    // non-null field, keys and values
    _results = ImmutableMap.copyOf(results);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean containsTicker(ExternalIdBundle ticker) {
    return _results.containsKey(ticker);
  }

  @Override
  public ImmutableSet<ExternalIdBundle> tickerSet() {
    return _results.keySet();
  }

  @Override
  public int size() {
    return _results.size();
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

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "DefaultImmutableLiveDataResults[size=" + _results.size() + "]";
  }

}
