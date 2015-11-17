/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * A single item of high-level market data identified by a key and valuation date.
 * <p>
 * This represents a single piece of market data.
 * The market data has typically been calibrated, such as a curve or surface.
 *
 * @see MarketDataKey
 * @see MarketDataId
 * @param <T>  the type of this market data
 */
public interface MarketDataValue<T extends MarketDataValue<T>> {

  /**
   * Gets the key used to obtain this market data.
   * <p>
   * A {@link MarketDataKey} is used to identify market data within a given context.
   * A system may have multiple market data values with the same key, and {@link MarketDataId}
   * is used to uniquely identify each.
   * <p>
   * For example, the concept of a USD discounting curve is represented by a {@code MarketDataKey}, but
   * the system may contain more than one actual USD discounting curve, each represented by a {@code MarketDataId}.
   * <p>
   * Note that the key should not include the valuation date.
   * 
   * @return the key used to identify this market data
   */
  public abstract MarketDataKey<T> getKey();

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

}
