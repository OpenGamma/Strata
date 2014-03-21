/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.tuple.Pair;

/**
 * Extension to the MarketDataSource that means an non-eager
 * source is able to capture and report back the market data
 * requests that have been made.
 */
public interface StrategyAwareMarketDataSource extends MarketDataSource {

  /**
   * Returns the market data requests that have been made of
   * this source since its instantiation. This is used with
   * non-eager data sources (@see #isEagerDataSource) so that
   * a bulk retrieval of all market data can be done.
   *
   * As this method will generally only be called for non-eager
   * data sources, eager data sources do not need to track requests
   * made of them and can just return an empty Set.
   *
   * @return the market data requests made since instantiation, not null
   */
  Set<Pair<ExternalIdBundle, FieldName>> getRequestedData();

  /**
   * Returns the market data that this source is already handling.
   *
   * As this method will generally only be called for non-eager
   * data sources, eager data sources do not need to track requests
   * made of them and can just return an empty Set.
   *
   * @return the market data that this source is already handling, not null
   */
  Set<Pair<ExternalIdBundle, FieldName>> getManagedData();
  
  StrategyAwareMarketDataSource createPrimedSource();
  
  boolean isCompatible(MarketDataSpecification specification);
  
  void dispose();
  
}
