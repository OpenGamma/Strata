/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

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
   * Request a single item of market data.
   *
   * @param requirement the item of market data being requested
   * @param valuationTime the valuation time for the market data (which may necessitate
   * retrieving it from a historical source)
   * @return a result object containing an indication of whether the data is (currently)
   * available and the value if it is.
   */
  Result<MarketDataValues> requestData(MarketDataRequirement requirement, ZonedDateTime valuationTime);


  /**
   * Request multiple items of market data.
   *
   * @param requirements the items of market data being requested
   * @return a result object containing an indication of whether each item of data is (currently)
   * available and the value if it is.
   */
  Result<MarketDataValues> requestData(Set<MarketDataRequirement> requirements);

  Result<MarketDataSeries> requestData(MarketDataRequirement requirement, LocalDateRange dateRange);

  Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange);

  Result<MarketDataSeries> requestData(MarketDataRequirement requirement, Period seriesPeriod);

  Result<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, Period seriesPeriod);
}
