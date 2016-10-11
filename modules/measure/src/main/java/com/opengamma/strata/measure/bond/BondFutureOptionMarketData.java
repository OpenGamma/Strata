/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import java.time.LocalDate;

import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.pricer.bond.BondFutureVolatilities;
import com.opengamma.strata.product.SecurityId;

/**
 * Market data for bond future options.
 * <p>
 * This interface exposes the market data necessary for pricing bond future options.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface BondFutureOptionMarketData {

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
   * Gets the lookup that provides access to bond future volatilities.
   * 
   * @return the bond future options lookup
   */
  public abstract BondFutureOptionMarketDataLookup getLookup();

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
  public abstract BondFutureOptionMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the volatilities for the specified security ID.
   * <p>
   * If the security ID is not found, an exception is thrown.
   *
   * @param securityId  the security ID
   * @return the volatilities for the security ID
   * @throws MarketDataNotFoundException if the security ID is not found
   */
  public abstract BondFutureVolatilities volatilities(SecurityId securityId);

}
