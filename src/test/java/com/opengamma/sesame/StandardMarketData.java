/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

public class StandardMarketData implements MarketData {

  private final MarketDataResultGenerator _resultGenerator;
  private final Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> _requirementStatus;

  public StandardMarketData(MarketDataResultGenerator resultGenerator,
                     Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> requirementStatus) {
    _resultGenerator = resultGenerator;
    _requirementStatus = requirementStatus;
  }

  @Override
  public MarketDataFunctionResult retrieveItems(Set<MarketDataRequirement> requiredMarketData) {

    Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> result = new HashMap<>();
    Set<MarketDataRequirement> missing = new HashSet<>();

    for (MarketDataRequirement requirement : requiredMarketData) {

      if (_requirementStatus.containsKey(requirement)) {
        result.put(requirement, _requirementStatus.get(requirement));
      }
      else {
        result.put(requirement, Pairs.of(MarketDataStatus.PENDING, (MarketDataValue) null));
        missing.add(requirement);
      }

    }

    return _resultGenerator.marketDataResultBuilder().foundData(result).missingData(missing).build();
  }

  @Override
  public MarketDataFunctionResult retrieveItem(MarketDataRequirement requiredMarketData) {
    return retrieveItems(ImmutableSet.of(requiredMarketData));
  }
}
