/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.FxIndexRates;
import com.opengamma.strata.market.curve.IborIndexRates;
import com.opengamma.strata.market.curve.OvernightIndexRates;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.BaseProvider;

/**
 * A provider of rates, such as Ibor and Overnight, used for pricing financial instruments.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * The valuation date, FX rates, discount factors, time-series and forward curves are included.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RatesProvider
    extends BaseProvider {

  /**
   * Gets additional market data of a specific type.
   * <p>
   * In general, it is desirable to pass the specific market data needed for pricing into
   * the pricing method. However, in some cases, notably swaps, this is not feasible.
   * This method can be used to access additional data of a specific type.
   * <pre>
   *   MarketVolatilityData vol = provider.data(MarketVolatilityData.class);
   * </pre>
   * It is strongly recommended to clearly state on pricing methods what additional data is required.
   * <p>
   * The specific methods on this interface for Ibor and Overnight indices exist because
   * they are common cases. The data could also be made available via this method.
   * 
   * @param type  the type of additional data to obtain
   * @return the additional data
   * @throws IllegalArgumentException if the additional data is not available
   */
  public abstract <T> T data(Class<T> type);

  /**
   * Gets the time series of an index.
   * <p>
   * Each index has a history of previously observed values, which can be obtained by this method.
   * 
   * @param index  the index to find a time series for
   * @return the time series of an index
   * @throws IllegalArgumentException if the time-series is not available
   */
  public abstract LocalDateDoubleTimeSeries timeSeries(Index index);

  //-------------------------------------------------------------------------
  /**
   * Gets the rates for an FX index.
   * <p>
   * This returns an object that can provide historic and forward rates for the specified index.
   * <p>
   * An FX rate is the conversion rate between two currencies. An FX index is the rate
   * as published by a specific organization, typically at a well-known time-of-day.
   * 
   * @param index  the index to find rates for
   * @return the rates for the specified index
   * @throws IllegalArgumentException if the rates are not available
   */
  public abstract FxIndexRates fxIndexRates(FxIndex index);

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
   * @throws IllegalArgumentException if the rates are not available
   */
  public default double fxIndexRate(FxIndex index, Currency baseCurrency, LocalDate fixingDate) {
    return fxIndexRates(index).rate(baseCurrency, fixingDate);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the rates for an Ibor index.
   * <p>
   * The rate of the Ibor index, such as 'GBP-LIBOR-3M', varies over time.
   * This returns an object that can provide historic and forward rates for the specified index.
   * 
   * @param index  the index to find rates for
   * @return the rates for the specified index
   * @throws IllegalArgumentException if the rates are not available
   */
  public abstract IborIndexRates iborIndexRates(IborIndex index);

  /**
   * Gets the historic or forward rate of an Ibor index.
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
   * @throws IllegalArgumentException if the rates are not available
   */
  public default double iborIndexRate(IborIndex index, LocalDate fixingDate) {
    return iborIndexRates(index).rate(fixingDate);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the rates for an Overnight index.
   * <p>
   * The rate of the Overnight index, such as 'EUR-EONIA', varies over time.
   * This returns an object that can provide historic and forward rates for the specified index.
   * 
   * @param index  the index to find rates for
   * @return the rates for the specified index
   * @throws IllegalArgumentException if the rates are not available
   */
  public abstract OvernightIndexRates overnightIndexRates(OvernightIndex index);

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
   * @throws IllegalArgumentException if the rates are not available
   */
  public default double overnightIndexRate(OvernightIndex index, LocalDate fixingDate) {
    return overnightIndexRates(index).rate(fixingDate);
  }

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
   * @return the sensitivity to the curve parameters
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
