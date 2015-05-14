/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.marketdata.id;

import com.opengamma.strata.basics.marketdata.key.MarketDataKey;

/**
 * An identifier for a unique item of market data.
 * <p>
 * This can identify a piece of observable market data, for example the quoted market value of a security.
 * It can also identify a high level item of market data that is derived from other data, for example
 * a volatility surface or a curve group containing calibrated curves.
 * <p>
 * Market data identifiers are used by the market data system for identifying data, they are not intended
 * to be used by function code. Functions request data using {@link MarketDataKey} and the keys are mapped
 * to identifiers by the market data system.
 *
 * @param <T>  the type of the market data identified by the identifier
 *
 * @see MarketDataKey
 */
public interface MarketDataId<T> {

  /**
   * Returns the type of market data that is being identified.
   *
   * @return the type of market data that is being identified
   */
  public abstract Class<T> getMarketDataType();
}
