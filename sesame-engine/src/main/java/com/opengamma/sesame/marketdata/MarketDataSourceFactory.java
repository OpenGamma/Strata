/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;

/**
 * Factory for {@link MarketDataSource}.
 */
public interface MarketDataSourceFactory {

  /**
   * Creates a {@link MarketDataSource} for a given specification.
   * 
   * @param spec  the market data specification, not null
   * @return the market data source, not null
   */
  MarketDataSource create(MarketDataSpecification spec);
  
}
