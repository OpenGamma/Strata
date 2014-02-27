/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import org.threeten.bp.Period;

import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Function providing market data to clients. When data is requested a value is returned containing the status
 * and possibly the value for every item that was been requested.
 * TODO blocking variant
 */
public interface MarketDataFn {

  /**
   * Request a single item of market data.
   *
   * @param requirement the item of market data being requested
   * @return a result object containing an indication of whether the data is (currently)
   * available and the value if it is.
   */
  Result<MarketDataValues> requestData(MarketDataRequirement requirement);

  /**
   * Request multiple items of market data.
   *
   * @param requirements the items of market data being requested
   * @return a result object containing an indication of whether each item of data is (currently)
   * available and the value if it is.
   */
  Result<MarketDataValues> requestData(Set<MarketDataRequirement> requirements);

  // REVIEW jonathan 2014-02-27 -- the methods above and below here feel like two different concepts which should be
  // separated - those below always imply historical data access whereas those above could source data from any
  // consistent snapshot.
  // Perhaps need to move those below to an interface like HistoricalMarketDataFn. Functions can then depend on this to
  // access historical data, while using the same MarketDataRequirements to describe the data.
    Result<MarketDataSeries> requestData(MarketDataRequirement requirement, LocalDateRange dateRange);

  Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange);

  Result<MarketDataSeries> requestData(MarketDataRequirement requirement, Period seriesPeriod);

  Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, Period seriesPeriod);

}
