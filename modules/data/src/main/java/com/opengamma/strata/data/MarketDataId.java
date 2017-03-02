/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

/**
 * An identifier for a unique item of market data.
 * <p>
 * The market data system can locate market data using implementations of this interface.
 * Implementations can identify any piece of market data.
 * This includes observable values, such as the quoted market value of a security, and derived
 * values, such as a volatility surface or a discounting curve.
 *
 * @param <T>  the type of the market data this identifier refers to
 */
public interface MarketDataId<T> {

  /**
   * Gets the type of data this identifier refers to.
   *
   * @return the type of the market data this identifier refers to
   */
  public abstract Class<T> getMarketDataType();

}
