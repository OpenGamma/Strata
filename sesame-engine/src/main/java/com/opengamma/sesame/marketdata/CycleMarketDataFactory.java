/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;

/**
 * Provides the market data for the cycle of a view. This allows for
 * the need of some views to access multiple market data sources
 * during their run (e.g. calculating curves over a set of historic
 * dates).
 */
public interface CycleMarketDataFactory {

  /**
   * Gets the primary market data source that is required for this cycle.
   *
   * @return the primary market data source for this cycle
   */
  MarketDataSource getPrimaryMarketDataSource();

  /**
   * Gets an appropriate market data source for the specified date. The
   * source to be returned will be decided in part by the configured
   * primary source.
   *
   * @param valuationDate the date to get a source for
   * @return a market data source for the date
   */
  MarketDataSource getMarketDataSourceForDate(ZonedDateTime valuationDate);

  /**
   * Create a copy of this CycleMarketDataFactory but with an
   * updated primary source.
   *
   * @param marketDataSpec  the source to use as the primary market data source
   * @return the new CycleMarketDataFactory
   */
  CycleMarketDataFactory withMarketDataSpecification(MarketDataSpecification marketDataSpec);

  /**
   * Create a copy of this CycleMarketDataFactory but where the
   * primary source has been primed.
   *
   * @return the new CycleMarketDataFactory
   */
  CycleMarketDataFactory withPrimedMarketDataSource();

}
