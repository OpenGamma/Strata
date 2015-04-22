/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * A provider of rates, such as Ibor and Overnight, used for pricing financial instruments.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * The valuation date, FX rates, discount factors, time-series and forward curves are included.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RatesProvider {

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

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
   * Gets the FX rate for a currency pair on the valuation date.
   * <p>
   * The rate returned is the rate from the base to counter as defined by the
   * specified currency pair - {@code 1 * base = fxRate * counter}.
   * 
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the current FX rate for the currency pair
   */
  public abstract double fxRate(Currency baseCurrency, Currency counterCurrency);

  /**
   * Gets the FX rate for a currency pair on the valuation date.
   * <p>
   * The rate returned is the rate from the base to counter as defined by the
   * specified currency pair - {@code 1 * base = fxRate * counter}.
   * 
   * @param currencyPair  the ordered currency pair defining the rate required
   * @return the current FX rate for the currency pair
   */
  public default double fxRate(CurrencyPair currencyPair) {
    return fxRate(currencyPair.getBase(), currencyPair.getCounter());
  }

  /**
   * Converts the currency of an amount.
   * <p>
   * The input amount is a set of amounts in one or more currencies. 
   * This converts the amount to the target currency.
   * This conversion uses the current FX rate as returned by {@link #fxRate(Currency, Currency)}.
   * 
   * @param amount  the amount to convert
   * @param targetCurrency  the currency to convert to
   * @return the converted amount
   */
  public default CurrencyAmount fxConvert(MultiCurrencyAmount amount, Currency targetCurrency) {
    ArgChecker.notNull(amount, "amount");
    ArgChecker.notNull(targetCurrency, "targetCurrency");
    return CurrencyAmount.of(targetCurrency, amount.stream()
        .mapToDouble(ca -> fxRate(ca.getCurrency(), targetCurrency) * ca.getAmount())
        .sum());
  }

  /**
   * Converts the currency of an amount.
   * <p>
   * This converts the specified amount to the target currency.
   * This conversion uses the current FX rate as returned by {@link #fxRate(Currency, Currency)}.
   * 
   * @param amount  the amount to convert
   * @param targetCurrency  the currency to convert to
   * @return the converted amount
   */
  public default CurrencyAmount fxConvert(CurrencyAmount amount, Currency targetCurrency) {
    ArgChecker.notNull(amount, "amount");
    ArgChecker.notNull(targetCurrency, "targetCurrency");
    if (amount.getCurrency().equals(targetCurrency)) {
      return amount;
    }
    return CurrencyAmount.of(targetCurrency, fxRate(amount.getCurrency(), targetCurrency) * amount.getAmount());
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
   * @param index  the index to find the rate for
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
   * @param index  the index to find the rate for
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   */
  public abstract double iborIndexRate(IborIndex index, LocalDate fixingDate);

  /**
   * Gets the basic curve sensitivity for the forward rate of an IBOR-like index.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * If a time-series was used, then there is no sensitivity.
   * Otherwise, the sensitivity has the value 1.
   * The sensitivity refers to the result of {@link #iborIndexRate(IborIndex, LocalDate)}.
   * 
   * @param index  the index to find the sensitivity for
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the point sensitivity of the rate
   */
  public abstract PointSensitivityBuilder iborIndexRateSensitivity(IborIndex index, LocalDate fixingDate);

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
   * @param index  the index to find the rate for
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   */
  public abstract double overnightIndexRate(OvernightIndex index, LocalDate fixingDate);

  /**
   * Gets the basic curve sensitivity for the forward rate of an Overnight index.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * If a time-series was used, then there is no sensitivity.
   * Otherwise, the sensitivity has the value 1.
   * The sensitivity refers to the result of {@link #overnightIndexRate(OvernightIndex, LocalDate)}.
   * 
   * @param index  the index to find the sensitivity for
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the point sensitivity of the rate
   */
  public PointSensitivityBuilder overnightIndexRateSensitivity(OvernightIndex index, LocalDate fixingDate);

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
   * @param index  the index to find the rate for
   * @param startDate  the start or effective date of the period on which the rate is computed
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the simply compounded rate associated to the period for the index
   * @throws IllegalArgumentException when data stored based on the fixing date and not
   *  the start and end date of the period
   */
  public abstract double overnightIndexRatePeriod(OvernightIndex index, LocalDate startDate, LocalDate endDate);

  /**
   * Gets the basic curve sensitivity for the forward rate of an Overnight index on a given period.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * The sensitivity will have the value 1.
   * The sensitivity refers to the result of {@link #overnightIndexRatePeriod(OvernightIndex, LocalDate, LocalDate)}.
   * 
   * @param index  the index to find the sensitivity for
   * @param startDate  the start or effective date of the period on which the rate is computed
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the point sensitivity of the rate
   * @throws IllegalArgumentException when data stored based on the fixing date and not
   *  the start and end date of the period
   */
  public PointSensitivityBuilder overnightIndexRatePeriodSensitivity(
      OvernightIndex index,
      LocalDate startDate,
      LocalDate endDate);

  //-------------------------------------------------------------------------
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurveParameterSensitivity} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the curve internal parameters representation.
   * <p>
   * For example, the point sensitivities could represent the sensitivity to a date on the first
   * of each month in a year relative to a specific forward curve. This method converts to the point
   * sensitivities to be relative to each parameter on the underlying curve, such as the 1 day, 1 week,
   * 1 month, 3 month, 12 month and 5 year nodal points.
   * 
   * @param pointSensitivities  the point sensitivity
   * @return  the sensitivity to the curve parameters
   */
  CurveParameterSensitivity parameterSensitivity(PointSensitivities pointSensitivities);

  //-------------------------------------------------------------------------
  /**
   * Converts a date to a relative {@code double} time.
   * <p>
   * This uses the day-count of the provider to determine the year fraction.
   * 
   * @param date  the date to find the relative time of
   * @return the relative time
   */
  public abstract double relativeTime(LocalDate date);

}
