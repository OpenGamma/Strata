/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.capfloor;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.pricer.capfloor.IborCapletFloorletVolatilities;

/**
 * Market data for Ibor cap/floor.
 * <p>
 * This interface exposes the market data necessary for pricing Ibor caps/floors.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface IborCapFloorMarketData {

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
   * Gets the lookup that provides access to cap/floor volatilities.
   * 
   * @return the cap/floor lookup
   */
  public abstract IborCapFloorMarketDataLookup getLookup();

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
  public abstract IborCapFloorMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the volatilities for the specified Ibor index.
   * <p>
   * If the index is not found, an exception is thrown.
   *
   * @param index  the Ibor index
   * @return the volatilities for the index
   * @throws MarketDataNotFoundException if the index is not found
   */
  public abstract IborCapletFloorletVolatilities volatilities(IborIndex index);

}
