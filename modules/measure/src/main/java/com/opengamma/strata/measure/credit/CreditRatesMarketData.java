/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import java.time.LocalDate;

import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;

/**
 * Market data for credit products.
 * <p>
 * This interface exposes the market data necessary for pricing credit products.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface CreditRatesMarketData {

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
   * Gets the lookup that provides access to credit, discount and recovery rate curves.
   * 
   * @return the lookup
   */
  public abstract CreditRatesMarketDataLookup getLookup();

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
  public abstract CreditRatesMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the credit rates provider.
   * <p>
   * This provides access to credit, discount and recovery rate curves.
   * 
   * @return the credit rates provider
   */
  public abstract CreditRatesProvider creditRatesProvider();

}
