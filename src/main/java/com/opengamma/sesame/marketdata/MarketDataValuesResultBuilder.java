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

/**
 *
 */
public class MarketDataValuesResultBuilder {

  // TODO should this be a map including the status?
  private final Set<MarketDataRequirement> _missing = new HashSet<>();

  private final Map<MarketDataRequirement, MarketDataItem> _results = new HashMap<>();

  /*public MarketDataValuesResultBuilder missingData(Set<MarketDataRequirement> missing) {
    for (MarketDataRequirement requirement : missing) {
      missingData(requirement);
    }
    return this;
  }*/

  public MarketDataValuesResultBuilder missingData(MarketDataRequirement requirement, MarketDataStatus status) {
    _missing.add(requirement);
    _results.put(requirement, MarketDataItem.missing(status));
    return this;

  }

  /*public MarketDataValuesResultBuilder foundData(Map<MarketDataRequirement, MarketDataItem> data) {
    _results.putAll(data);
    return this;
  }*/

  public MarketDataValuesResultBuilder foundData(MarketDataRequirement requirement, MarketDataItem item) {
    _results.put(requirement, item);
    return this;
  }

  // TODO what's the correct behaviour here? is getting some of the data success or failure?
  public Result<MarketDataValues> build() {
    if (_results.isEmpty()) {
      return ResultGenerator.failure(FailureStatus.MISSING_DATA, "Missing market data: {}", _missing);
    } else {
      return ResultGenerator.success(new MarketDataValues(_results, _missing));
    }
  }
}
