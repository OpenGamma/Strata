/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.curve.DiscountFactors;

/**
 * A provider of data used for pricing.
 * <p>
 * This provides the valuation date, FX rates and discount factors,
 * Sensitivity for discount factors is also available.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface BaseProvider
    extends FxRateProvider {

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  //-------------------------------------------------------------------------
  /**
   * Gets the FX rate for the specified currency pair on the valuation date.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * 
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the current FX rate for the currency pair
   * @throws IllegalArgumentException if the rate is not available
   */
  @Override
  public abstract double fxRate(Currency baseCurrency, Currency counterCurrency);

  /**
   * Gets the FX rate for the specified currency pair on the valuation date.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * 
   * @param currencyPair  the ordered currency pair defining the rate required
   * @return the current FX rate for the currency pair
   * @throws IllegalArgumentException if the rate is not available
   */
  @Override
  public default double fxRate(CurrencyPair currencyPair) {
    return fxRate(currencyPair.getBase(), currencyPair.getCounter());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the discount factors for a currency.
   * <p>
   * The discount factor represents the time value of money for the specified currency
   * when comparing the valuation date to the specified date.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param currency  the currency to get the discount factors for
   * @return the discount factors for the specified currency
   * @throws IllegalArgumentException if the discount factors are not available
   */
  public abstract DiscountFactors discountFactors(Currency currency);

  /**
   * Gets the discount factor applicable for a currency.
   * <p>
   * The discount factor represents the time value of money for the specified currency
   * when comparing the valuation date to the specified date.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param currency  the currency to get the discount factor for
   * @param date  the date to discount to
   * @return the discount factor
   * @throws IllegalArgumentException if the discount factors are not available
   */
  public default double discountFactor(Currency currency, LocalDate date) {
    return discountFactors(currency).discountFactor(date);
  }

}
