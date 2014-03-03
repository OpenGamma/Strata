/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Function providing market data to clients. When data is requested a value is returned containing the status
 * and possibly the value for every item that was been requested.
 */
public interface HistoricalMarketDataFn {

  Result<MarketDataSeries> requestData(MarketDataRequirement requirement, LocalDateRange dateRange);

  Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange);
}
