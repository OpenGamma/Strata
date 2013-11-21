/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;
import java.util.Set;

import com.opengamma.util.tuple.Pair;

public interface MarketDataResultBuilder {

  MarketDataResultBuilder missingData(Set<MarketDataRequirement> missing);

  MarketDataResultBuilder missingData(MarketDataRequirement requirement);

  MarketDataResultBuilder foundData(Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> result);

  MarketDataResultBuilder foundData(MarketDataRequirement requirement,
                 Pair<MarketDataStatus, ? extends MarketDataValue> state);

  MarketDataFunctionResult build();
}
