/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import java.time.LocalDate;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.basics.index.FxIndex;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.basics.index.Index;
import com.opengamma.basics.index.OvernightIndex;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * The pricing environment used to calculate analytic measures.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * This includes FX rates, discount factors and forward curves.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface PricingEnvironment {

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this environment is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  /**
   * Gets the raw pricing data.
   * <p>
   * This method allows for raw data to be obtained without adding to this interface.
   * 
   * @param cls  the type of raw data to retrieve
   * @return the raw data
   */
  public abstract <T> T rawData(Class<T> cls);

  /**
   * Gets the time series of an index.
   * <p>
   * Each index has a history of previously observed values, which can be obtained by this method.
   * 
   * @param index  the index to find a time series for
   * @return the time series of an index
   */
  public abstract LocalDateDoubleTimeSeries timeSeries(Index index);

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

  //-------------------------------------------------------------------------
  /**
   * Gets the FX rate for a currency pair on the valuation date.
   * <p>
   * The rate returned is the rate from the base to counter as defined by the
   * specified currency pair - {@code 1 * base = fxRate * counter}.
   * 
   * @param currencyPair  the ordered currency pair defining the rate required
   * @return the current FX rate for the currency pair
   */
  public abstract double fxRate(CurrencyPair currencyPair);

  /**
   * Converts the currency of an amount.
   * <p>
   * The input amount is a set of amounts in one or more currencies. 
   * This conversion returns the sum of the amount in each currency converted
   * to the specified currency using the current FX rate.
   * 
   * @param amount  the amount to convert
   * @param currency  the currency to convert to
   * @return the converted amount
   */
  public abstract CurrencyAmount fxConvert(MultiCurrencyAmount amount, Currency currency);

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate of an FX rate for a currency pair.
   * <p>
   * The rate of the FX index varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * <p>
   * The index defines the conversion rate for a specific currency pair.
   * This method specifies which of the two currencies in the index is to be treated
   * as the base currency for the purposes of the returned rate.
   * If the specified base currency equals the base currency of the index, then
   * the rate is simply returned. If the specified base currency equals the counter currency
   * of the index, then the inverse rate is returned.
   * As such, an amount in the specified base currency can be directly multiplied by the
   * returned FX rate to perform FX conversion.
   * <p>
   * To convert an amount in the specified base currency to the other currency,
   * multiply it by the returned FX rate.
   * 
   * @param index  the index to lookup
   * @param baseCurrency  the base currency that the rate should be expressed against
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   */
  public abstract double fxIndexRate(FxIndex index, Currency baseCurrency, LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate of an IBOR-like index.
   * <p>
   * The rate of the IBOR-like index, such as 'GBP-LIBOR-3M', varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * 
   * @param index  the index to lookup
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   */
  public abstract double iborIndexRate(IborIndex index, LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate of an Overnight index.
   * <p>
   * The rate of the overnight index, such as 'EUR-EONIA', varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * The reference period for the underlying deposit is computed from the index conventions.
   * 
   * @param index  the overnight index to lookup
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   */
  public abstract double overnightIndexRate(OvernightIndex index, LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Gets the forward rate of an overnight index on a given period, potentially different from an overnight period.
   * <p>
   * The start date should be on or after the valuation date. The end date should be after the start date.
   * <p>
   * This computes the forward rate in the simple simply compounded convention of the index between two given date.
   * This is used mainly to speed-up computation by computing the rate on a longer period instead of each individual 
   * overnight rate. When data related to the overnight index rate are stored based on the fixing date and not
   * the start and end date of the period, the call may return an {@code IllegalArgumentException}.
   * 
   * @param index  the overnight index to lookup
   * @param startDate  the start or effective date of the period on which the rate is computed
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the simply compounded rate associated to the period for the index
   * @throws IllegalArgumentException when data stored based on the fixing date and not
   *  the start and end date of the period
   */
  public abstract double overnightIndexRatePeriod(OvernightIndex index, LocalDate startDate, LocalDate endDate);

  //-------------------------------------------------------------------------
  /**
   * Converts a date to a relative {@code double} time.
   * <p>
   * This uses the day-count of the environment to determine the year fraction.
   * 
   * @param date  the date to find the relative time of
   * @return the relative time
   */
  public abstract double relativeTime(LocalDate date);

}
