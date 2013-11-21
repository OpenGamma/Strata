/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;
import java.util.Set;

public interface MarketDataResultBuilder {

  MarketDataResultBuilder missingData(Set<MarketDataRequirement> missing);

  MarketDataResultBuilder missingData(MarketDataRequirement requirement);

  MarketDataResultBuilder foundData(Map<MarketDataRequirement, MarketDataItem<?>> data);

  // TODO is this a good name given that this is what we'll call if the data is missing or pending?
  MarketDataResultBuilder foundData(MarketDataRequirement requirement, MarketDataItem<?> value);

  MarketDataSingleResult build();
}
