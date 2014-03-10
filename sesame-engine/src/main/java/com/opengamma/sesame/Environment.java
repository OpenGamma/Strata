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
   * Gets the valuation date.
   * <p>
   * This method should be used in preference to {@link #getValuationTime()} if only the date is required.
   * 
   * @return the valuation date, not null
   */
  LocalDate getValuationDate();

  /**
   * Gets the valuation time.
   * <p>
   * Use {@link #getValuationDate()} in preference to this method if you only need the date.
   * 
   * @return the valuation time, not null
   */
  ZonedDateTime getValuationTime();

  /**
   * Gets the source used to access market data.
   *
   * @return the market data source, not null
   */
  MarketDataSource getMarketDataSource();

  /**
   * Returns a new environment copied from this environment but with a different valuation time.
   *
   * @param valuationTime  the valuation time for the new environment, not null
   * @return a new environment copied from this environment but with the specified valuation time, not null
   */
  Environment withValuationTime(ZonedDateTime valuationTime);

  /**
   * Returns a new environment copied from this environment but with a different valuation time.
   *
   * @param marketData  the market data for the new environment, not null
   * @return a new environment copied from this environment but with the specified market data, not null
   */
  Environment withMarketData(MarketDataSource marketData);

  /**
   * Returns a new environment with a different valuation time and market data.
   *
   * @param marketData  the market data for the new environment, not null
   * @param valuationTime  the valuation time for the new environment, not null
   * @return a new environment, not null
   */
  Environment with(ZonedDateTime valuationTime, MarketDataSource marketData);

  // TODO builder? depends how many more things we add to this
}
