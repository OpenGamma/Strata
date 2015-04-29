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
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

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
   * @throws RuntimeException if no FX rate could be found
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
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public default double fxRate(CurrencyPair currencyPair) {
    return fxRate(currencyPair.getBase(), currencyPair.getCounter());
  }

  //-------------------------------------------------------------------------
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
   */
  public abstract double discountFactor(Currency currency, LocalDate date);

  /**
   * Gets the zero rate curve sensitivity for the discount factor.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity typically has the value {@code (-discountFactor * relativeTime)}.
   * The sensitivity refers to the result of {@link #discountFactor(Currency, LocalDate)}.
   * 
   * @param currency  the currency to get the sensitivity for
   * @param date  the date to discount to
   * @return the point sensitivity of the zero rate
   */
  public abstract PointSensitivityBuilder discountFactorZeroRateSensitivity(Currency currency, LocalDate date);

}
