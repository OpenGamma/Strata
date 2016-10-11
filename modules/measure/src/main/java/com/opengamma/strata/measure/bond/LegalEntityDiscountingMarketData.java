/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import java.time.LocalDate;

import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;

/**
 * Market data for products based on repo and issuer curves.
 * <p>
 * This interface exposes the market data necessary for pricing bond products,
 * such as fixing coupon bonds, capital indexed bonds and bond futures.
 * It uses a {@link LegalEntityDiscountingMarketDataLookup} to provide a view on {@link MarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface LegalEntityDiscountingMarketData {

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
   * Gets the lookup that provides access to repo and issuer curves.
   * 
   * @return the discounting lookup
   */
  public abstract LegalEntityDiscountingMarketDataLookup getLookup();

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
  public abstract LegalEntityDiscountingMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the discounting provider.
   * <p>
   * This provides access to repo and issuer curves.
   * 
   * @return the discounting provider
   */
  public abstract LegalEntityDiscountingProvider discountingProvider();

}
