/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

/**
 * An identifier for a unique item of market data.
 * <p>
 * The market data system can locate market data using implementations of this interface.
 * Implementations can identify any piece of market data.
 * This includes observable values, such as the quoted market value of a security, and derived
 * values, such as a volatility surface or a discounting curve.
 * <p>
 * Note that this interface is typically used by the market data system and not by applications.
 * Application code will generally use {@link MarketDataKey}, with a mapping step to convert
 * the key to a {@code MarketDataId}.
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

  /**
   * Returns the key associated with this ID.
   *
   * @return the key associated with this ID
   */
  public abstract MarketDataKey<T> toMarketDataKey();
}
