/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.pricer.index.OvernightFutureOptionVolatilities;

/**
 * Market data for Overnight future options.
 * <p>
 * This interface exposes the market data necessary for pricing an Overnight future option.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface OvernightFutureOptionMarketData {

  /**
   * Gets the valuation date.
   *
   * @return the valuation date
   */
  public default LocalDate getValuationDate() {
    return getMarketData().getValuationDate();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the lookup that provides access to Overnight future option volatilities.
   *
   * @return the Overnight future option lookup
   */
  public abstract OvernightFutureOptionMarketDataLookup getLookup();

  /**
   * Gets the market data.
   *
   * @return the market data
   */
  public abstract MarketData getMarketData();

  /**
   * Returns a copy of this instance with the specified market data.
   *
   * @param marketData  the market data to use
   * @return a market view based on the specified data
   */
  public abstract OvernightFutureOptionMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the volatilities for the specified Overnight index.
   * <p>
   * If the index is not found, an exception is thrown.
   *
   * @param index  the Overnight index
   * @return the volatilities for the index
   * @throws MarketDataNotFoundException if the index is not found
   */
  public abstract OvernightFutureOptionVolatilities volatilities(OvernightIndex index);

}
