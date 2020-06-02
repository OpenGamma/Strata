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
public interface MarketDataId<T> extends Comparable<MarketDataId<?>> {

  /**
   * Gets the type of data this identifier refers to.
   *
   * @return the type of the market data this identifier refers to
   */
  public abstract Class<T> getMarketDataType();

  /**
   * Compares two market data identifiers.
   * <p>
   * This compares the identifiers using their {@code toString} form.
   * It is intended that the string fully represents the state of the identifier.
   * It is recommended that the string starts with the short class name of the implementing class and a colon.
   * For example, {@link FxRateId#toString()} returns 'FxRate:{currencyPair}'.
   * 
   * @param other  the other identifier
   * @return the comparison
   */
  @Override
  public default int compareTo(MarketDataId<?> other) {
    return toString().compareTo(other.toString());
  }

}
