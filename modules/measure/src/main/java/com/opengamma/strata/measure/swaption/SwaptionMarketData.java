/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;

/**
 * Market data for swaptions.
 * <p>
 * This interface exposes the market data necessary for pricing a swaption.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface SwaptionMarketData {

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
   * Gets the lookup that provides access to swaption volatilities.
   * 
   * @return the swaption lookup
   */
  public abstract SwaptionMarketDataLookup getLookup();

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
  public abstract SwaptionMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the volatilities for the specified index.
   * <p>
   * If the index is not found, an exception is thrown.
   *
   * @param index  the index
   * @return the volatilities for the index
   * @throws MarketDataNotFoundException if the index is not found
   */
  public abstract SwaptionVolatilities volatilities(RateIndex index);

}
