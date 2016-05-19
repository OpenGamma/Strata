/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import java.time.LocalDate;

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

}
