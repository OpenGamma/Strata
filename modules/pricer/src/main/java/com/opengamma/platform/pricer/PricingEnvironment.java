/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import java.time.LocalDate;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.basics.index.FxIndex;
import com.opengamma.basics.index.Index;
import com.opengamma.basics.index.RateIndex;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * The pricing environment used to calculate analytic measures.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface PricingEnvironment {

  /**
   * Gets the multicurve data.
   * 
   * @return the multicurve data
   */
  public MulticurveProviderInterface getMulticurve();

  /**
   * Gets the time series of an index.
   * 
   * @param index  the index to find a time series for
   * @return the time series of an index
   */
  public LocalDateDoubleTimeSeries getTimeSeries(Index index);

  /**
   * Gets the discount factor of a date.
   * 
   * @param currency  the currency to apply the discount factor in
   * @param valuationDate  the valuation date
   * @param date  the date to discount to
   * @return the discount factor
   */
  public double discountFactor(Currency currency, LocalDate valuationDate, LocalDate date);

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate of a rate index.
   * 
   * @param index  the index to lookup
   * @param valuationDate  the valuation date
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   */
  public double indexRate(RateIndex index, LocalDate valuationDate, LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Gets the current FX rate for a currency pair.
   * <p>
   * The rate returned is the rate from the base to counter as defined in the currency pair.
   * 
   * @param currencyPair  the ordered currency pair defining the rate required
   * @return the current FX rate for the currency pair
   */
  public double fxRate(CurrencyPair currencyPair);

  /**
   * Gets the historic or forward rate of an FX rate for a currency pair.
   * <p>
   * The rate returned is the rate from the base to counter as defined in the currency pair.
   * 
   * @param index  the index to lookup
   * @param currencyPair  the ordered currency pair defining the rate required
   * @param valuationDate  the valuation date
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   */
  public double fxRate(FxIndex index, CurrencyPair currencyPair, LocalDate valuationDate, LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Converts a date to a relative {@code double} time.
   * <p>
   * This uses the day-count of the environment to determine the year fraction.
   * 
   * @param baseDate  the base date to find the time relative to
   * @param date  the date to find the relative time of
   * @return the relative time
   */
  public double relativeTime(LocalDate baseDate, LocalDate date);

}
