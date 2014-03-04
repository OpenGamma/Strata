/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.DataNotFoundException;
import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 * TODO can this be merged with MarketDataSeries? the only difference is casting the results
 * @deprecated use {@link MarketDataFn2}
 */
@Deprecated
public class MarketDataValues {

  private final Map<MarketDataRequirement, MarketDataItem> _results;
    // TODO does this need to be map<requirement, status>? or collection<pair<requirement, status>>?
  private final Set<MarketDataRequirement> _missing;

  public MarketDataValues(Map<MarketDataRequirement, MarketDataItem> results, Set<MarketDataRequirement> missing) {
    _results = ImmutableMap.copyOf(ArgumentChecker.notNull(results, "results"));
    _missing = ImmutableSet.copyOf(ArgumentChecker.notNull(missing, "missing"));
  }

  // TODO should this throw an exception if there's no value? I think so. check status if you want to know?
  public Object getValue(MarketDataRequirement requirement) {
    MarketDataItem item = _results.get(requirement);
    if (item == null) {
      throw new DataNotFoundException("No value found for requirement " + requirement);
    }
    return item.getValue();
  }

  public MarketDataStatus getStatus(MarketDataRequirement requirement) {
    MarketDataItem item = _results.get(requirement);
    if (item == null) {
      throw new DataNotFoundException("No value found for requirement " + requirement);
    }
    return item.getStatus();
  }

  public Object getOnlyValue() {
    if (_results.size() != 1) {
      throw new IllegalStateException("Can't getOnlyValue because there are " + _results.size() + " values");
    }
    return _results.values().iterator().next().getValue();
  }

  public Iterable<Pair<MarketDataRequirement, MarketDataStatus>> getMissingValues() {
    throw new UnsupportedOperationException();
  }

  /**
   * Temporary method to allow conversion to the old-style market data bundle.
   *
   * @return a snapshot bundle with the data from the result
   */
  public SnapshotDataBundle toSnapshot() {
    SnapshotDataBundle snapshot = new SnapshotDataBundle();
    for (Map.Entry<MarketDataRequirement, MarketDataItem> entry : _results.entrySet()) {
      MarketDataRequirement key = entry.getKey();
      MarketDataItem item = entry.getValue();
      MarketDataStatus status = item.getStatus();
      if (key instanceof CurveNodeMarketDataRequirement && status == MarketDataStatus.AVAILABLE) {
        Object marketDataValue = item.getValue();
        snapshot.setDataPoint(((CurveNodeMarketDataRequirement) key).getExternalId(), (Double) marketDataValue);
      }
    }
    return snapshot;
  }

  @Override
  public String toString() {
    return "MarketDataValues [_results=" + _results + ", _missing=" + _missing + "]";
  }
}
