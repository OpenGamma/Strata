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
import com.opengamma.timeseries.date.DateTimeSeries;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 *
 */
public class MarketDataSeries {

  private final Map<MarketDataRequirement, MarketDataItem> _results;
  private final Set<MarketDataRequirement> _missing;

  public MarketDataSeries(Map<MarketDataRequirement, MarketDataItem> results, Set<MarketDataRequirement> missing) {
    // TODO check results only contains time series
    _results = ImmutableMap.copyOf(ArgumentChecker.notNull(results, "results"));
    _missing = ImmutableSet.copyOf(ArgumentChecker.notNull(missing, "missing"));
  }

  // TODO should this throw an exception if there's no value? I think so. check status if you want to know?
  public DateTimeSeries<LocalDate, ?> getSeries(MarketDataRequirement requirement) {
    throw new UnsupportedOperationException();

  }

  public MarketDataStatus getStatus(MarketDataRequirement requirement) {
    throw new UnsupportedOperationException();

  }

  public DateTimeSeries<LocalDate, ?> getOnlySeries() {
    throw new UnsupportedOperationException();

  }

  public Iterable<Pair<MarketDataRequirement, MarketDataStatus>> getMissingValues() {
    throw new UnsupportedOperationException();

  }
}
