/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;
import java.util.Set;

import org.threeten.bp.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.DataNotFoundException;
import com.opengamma.timeseries.date.DateTimeSeries;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 * TODO can this be merged with MarketDataValues? the only difference is casting the results
 * but what about the type params? how could we capture those in the signature? only by overriding all the methods
 * and casting the result of calling the superclass method?
 */
@SuppressWarnings("unchecked")
public class MarketDataSeries {

  private final Map<MarketDataRequirement, MarketDataItem> _results;
  // TODO does this need to be map<requirement, status>? or collection<pair<requirement, status>>?
  private final Set<MarketDataRequirement> _missing;

  public MarketDataSeries(Map<MarketDataRequirement, MarketDataItem> results, Set<MarketDataRequirement> missing) {
    // TODO check results only contains time series
    _results = ImmutableMap.copyOf(ArgumentChecker.notNull(results, "results"));
    _missing = ImmutableSet.copyOf(ArgumentChecker.notNull(missing, "missing"));
  }

  // TODO is the return type right? will we ever return anything except LocalDateDoubleTimeSeries?
  public DateTimeSeries<LocalDate, ?> getSeries(MarketDataRequirement requirement) {
    MarketDataItem item = _results.get(requirement);
    if (item == null) {
      throw new DataNotFoundException("No value found for requirement " + requirement);
    }
    return (DateTimeSeries<LocalDate, ?>) item.getValue();
  }

  public MarketDataStatus getStatus(MarketDataRequirement requirement) {
    MarketDataItem item = _results.get(requirement);
    if (item == null) {
      throw new DataNotFoundException("No value found for requirement " + requirement);
    }
    return item.getStatus();
  }

  // TODO is the return type right? will we ever return anything except LocalDateDoubleTimeSeries?
  public DateTimeSeries<LocalDate, ?> getOnlySeries() {
    if (_results.size() != 1) {
      throw new IllegalStateException("Can't getOnlyValue because there are " + _results.size() + " values");
    }
    return (DateTimeSeries<LocalDate, ?>) _results.values().iterator().next().getValue();
  }

  public Iterable<Pair<MarketDataRequirement, MarketDataStatus>> getMissingValues() {
    throw new UnsupportedOperationException();

  }
}
