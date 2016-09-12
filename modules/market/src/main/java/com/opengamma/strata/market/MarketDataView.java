/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.data.MarketDataName;

/**
 * A high-level view of a single item of market data.
 * <p>
 * Implementations provide a high-level view of a single piece of market data.
 * The market data has typically been calibrated, such as a curve or surface.
 * The data is valid on a single valuation date.
 */
public interface MarketDataView {

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  /**
   * Finds the market data with the specified name.
   * <p>
   * This is most commonly used to find an underlying curve or surface by name.
   * If the market data cannot be found, empty is returned.
   * 
   * @param <T>  the type of the market data value
   * @param name  the name to find
   * @return the market data value, empty if not found
   */
  public abstract <T> Optional<T> findData(MarketDataName<T> name);

}
