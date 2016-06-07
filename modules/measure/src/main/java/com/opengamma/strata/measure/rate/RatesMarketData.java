/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Market data for rates products.
 * <p>
 * This interface exposes the market data necessary for pricing rates products,
 * such as Swaps, FRAs and FX.
 * It uses a {@link RatesMarketDataLookup} to provide a view on {@link MarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface RatesMarketData {

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
   * Gets the lookup that provides access to discount curves and forward curves.
   * 
   * @return the rates lookup
   */
  public abstract RatesMarketDataLookup getLookup();

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
  public abstract RatesMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the rates provider.
   * <p>
   * This provides access to discount curves, forward curves and FX.
   * 
   * @return the rates provider
   */
  public abstract RatesProvider ratesProvider();

  /**
   * Gets the FX rate provider.
   * <p>
   * This provides access to FX rates.
   * By default, this returns the rates provider.
   * 
   * @return the rates provider
   */
  public default FxRateProvider fxRateProvider() {
    return ratesProvider();
  }

}
