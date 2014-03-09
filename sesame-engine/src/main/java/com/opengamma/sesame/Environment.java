/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.sesame.marketdata.MarketDataSource;

/**
 * The execution environment for functions, includes the valuation time and market data.
 */
public interface Environment {

  /**
   * Returns the valuation date.
   * This method should be used in preference to {@link #getValuationTime()} if only the date is required
   * @return the valuation date, not null
   */
  LocalDate getValuationDate();

  /**
   * Returns the valuation time.
   * Use {@link #getValuationDate()} in preference to this method if you only need the date.
   * @return the valuation time, not null
   */
  ZonedDateTime getValuationTime();

  /**
   * Request an item of market data.
   *
   * @return a result object containing an indication of whether the data is (currently)
   * available and the value if it is.
   */
  MarketDataSource getMarketDataSource();

  /**
   * Returns a new environment copied from this environment but with a different valuation time.
   *
   * @param valuationTime the valuation time for the new environment
   * @return a new environment copied from this environment but with the specified valuation time.
   */
  Environment withValuationTime(ZonedDateTime valuationTime);

  /**
   * Returns a new environment copied from this environment but with a different valuation time.
   *
   * @param marketData the market data for the new environment
   */
  Environment withMarketData(MarketDataSource marketData);

  /**
   * Returns a new environment with a different valuation time and market data.
   *
   * @param marketData the market data for the new environment
   * @param valuationTime the valuation time for the new environment
   * @return a new environment
   */
  Environment with(ZonedDateTime valuationTime, MarketDataSource marketData);

  // TODO builder? depends how many more things we add to this
}
