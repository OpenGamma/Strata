/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO this is highly provisional
 * TODO should this return MarketDataFunctionResult or something similar that indicates it's a time series?
 */
public interface MarketDataSeriesProviderFunction {

  MarketDataFunctionResult requestData(MarketDataRequirement requirement, LocalDateRange dateRange);

  MarketDataFunctionResult requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange);

}
