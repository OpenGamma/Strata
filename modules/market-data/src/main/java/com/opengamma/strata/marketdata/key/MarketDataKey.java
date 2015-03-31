/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.key;

/**
 * An identifier for an item of market data required for a calculation.
 * <p>
 * When a function requires market data it creates a market data key for the data and uses it to
 * request the data from the market data system.
 * <p>
 * A market data key doesn't necessarily identify a unique item of data. For example, if a calculation
 * requires a USD discounting curve it creates a discounting curve key and provides USD as the currency.
 * The system might contain any number of USD discounting curves and the calculation configuration provides a
 * mapping to resolved a key into a unique identifier. In the case of a discounting curve, the mapping
 * specifies the name of the curve group that contains the curve.
 *
 * @param <T>  the type of the market data identified by the key
 */
public interface MarketDataKey<T> {

  /**
   * Returns the type of market data identified by the key.
   *
   * @return the type of market data identified by the key
   */
  public abstract Class<T> getMarketDataType();

}
