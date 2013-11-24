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

import com.opengamma.sesame.FunctionResult;
import com.opengamma.sesame.StandardResultGenerator;
import com.opengamma.timeseries.date.DateTimeSeries;

/**
 *
 */
public class MarketDataSeriesResultBuilder {

  private final Set<MarketDataRequirement> _missing = new HashSet<>();

  private final Map<MarketDataRequirement, MarketDataItem> _results = new HashMap<>();

  /*public MarketDataValuesResultBuilder missingData(Set<MarketDataRequirement> missing) {
    for (MarketDataRequirement requirement : missing) {
      missingData(requirement);
    }
    return this;
  }*/

  public MarketDataSeriesResultBuilder missingData(MarketDataRequirement requirement) {
    _missing.add(requirement);
    // TODO why pending? couldn't it also be UNAVAILABLE? or should it be always be UNAVAILABLE
    _results.put(requirement, MarketDataItem.PENDING);
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

  public FunctionResult<MarketDataSeries> build() {
    return StandardResultGenerator.success(new MarketDataSeries(_results, _missing));
  }
}
