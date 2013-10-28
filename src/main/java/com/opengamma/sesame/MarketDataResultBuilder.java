/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Map;
import java.util.Set;

import com.opengamma.util.tuple.Pair;

public interface MarketDataResultBuilder {

  MarketDataResultBuilder missingData(Set<MarketDataRequirement> missing);


  MarketDataResultBuilder foundData(Map<MarketDataRequirement, Pair<MarketDataStatus,? extends MarketDataValue>> result);

  MarketDataFunctionResult build();
}
