/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;

/**
 * Market data for FX options.
 * <p>
 * This interface exposes the market data necessary for pricing FX options.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface FxOptionMarketData {

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
   * Gets the lookup that provides access to FX options volatilities.
   * 
   * @return the FX options lookup
   */
  public abstract FxOptionMarketDataLookup getLookup();

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
  public abstract FxOptionMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the volatilities for the specified currency pair.
   * <p>
   * If the currency pair is not found, an exception is thrown.
   *
   * @param currencyPair  the currency pair
   * @return the volatilities for the currency pair
   * @throws MarketDataNotFoundException if the currency pair is not found
   */
  public abstract FxOptionVolatilities volatilities(CurrencyPair currencyPair);

}
