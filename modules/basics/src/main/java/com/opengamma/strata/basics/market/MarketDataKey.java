/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

/**
 * A key that identifies an item of market data.
 * <p>
 * An application can request market data using implementations of this interface.
 * Implementations can identify any piece of market data.
 * This includes observable values, such as the quoted market value of a security, and derived
 * values, such as a volatility surface or a discounting curve.
 * <p>
 * For example, to request a USD discounting curve, an application must create an instance of
 * a class implementing this interface that represents a request for a discounting curve.
 * The key implementation will store the desired currency, USD in this case.
 * <p>
 * A market data key does not necessarily identify a unique item of data.
 * A simple market data implementation will have a single value associated with the key.
 * A more complex system may have multiple values associated, which will involve a mapping
 * step to convert the {@code MarketDataKey} to a {@link MarketDataId}.
 * For example, the mapping step might involve selecting the curve group which supplied a curve.
 *
 * @param <T>  the type of the market data identified by the key
 */
public interface MarketDataKey<T> {

  /**
   * Gets the type of market data identified by the key.
   *
   * @return the type of market data identified by the key
   */
  public abstract Class<T> getMarketDataType();

}
