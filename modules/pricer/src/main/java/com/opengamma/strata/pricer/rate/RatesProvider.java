/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.value.FxIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.OvernightIndexRates;
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

  //-------------------------------------------------------------------------
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurveParameterSensitivities} associated with the {@link PointSensitivities}.
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
  CurveParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities);

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
