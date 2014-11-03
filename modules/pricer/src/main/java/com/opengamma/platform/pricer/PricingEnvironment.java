/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import java.time.LocalDate;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.index.RateIndex;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Pricing environment.
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
  public LocalDateDoubleTimeSeries getTimeSeries(RateIndex index);

  /**
   * Gets the discount factor of a date.
   * 
   * @param currency  the currency to apply the discount factor in
   * @param valuationDate  the valuation date
   * @param date  the date to discount to
   * @return the discount factor
   */
  public double discountFactor(Currency currency, LocalDate valuationDate, LocalDate date);

  /**
   * Gets the historic or forward rate of an index.
   * 
   * @param index  the index to lookup
   * @param valuationDate  the valuation date
   * @param fixingDate  the fixing date to query the index for
   * @return the rate of the index, either historic or forward
   */
  public double indexRate(RateIndex index, LocalDate valuationDate, LocalDate fixingDate);

  /**
   * Gets the time series of an index.
   * 
   * @param baseDate  the base date to find the time relative to
   * @param date  the date to find the relative time of
   * @return the relative time
   */
  public double relativeTime(LocalDate baseDate, LocalDate date);

}
