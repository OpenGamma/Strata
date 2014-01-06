/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.timeseries.date.DateTimeSeries;

/**
 *
 */
public class MarketDataSeriesResultBuilder {

  // TODO does this need to include the status?
  private final Set<MarketDataRequirement> _missing = new HashSet<>();

  private final Map<MarketDataRequirement, MarketDataItem> _results = new HashMap<>();

  /*public MarketDataValuesResultBuilder missingData(Set<MarketDataRequirement> missing) {
    for (MarketDataRequirement requirement : missing) {
      missingData(requirement);
    }
    return this;
  }*/

  public MarketDataSeriesResultBuilder missingData(MarketDataRequirement requirement, MarketDataStatus status) {
    _missing.add(requirement);
    _results.put(requirement, MarketDataItem.missing(status));
    return this;

  }

  /*public MarketDataValuesResultBuilder foundData(Map<MarketDataRequirement, MarketDataItem> data) {
    _results.putAll(data);
    return this;
  }*/

  public MarketDataSeriesResultBuilder foundData(MarketDataRequirement requirement, MarketDataItem item) {
    // TODO more thorough checks. is it a local date time series?
    if (!(item.getValue() instanceof DateTimeSeries)) {
      throw new IllegalArgumentException("Value must be a time series but is " + item.getValue());
    }
    _results.put(requirement, item);
    return this;
  }

  // TODO what's the correct behaviour here? is getting some of the data success or failure?
  public Result<MarketDataSeries> build() {
    if (!_results.isEmpty()) {
      return ResultGenerator.success(new MarketDataSeries(_results, _missing));
    } else {
      return ResultGenerator.failure(FailureStatus.MISSING_DATA, "Missing market data: {}", _missing);
    }
  }
}
