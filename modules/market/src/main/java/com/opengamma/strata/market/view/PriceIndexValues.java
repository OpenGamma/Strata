/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to the values of a price index.
 * <p>
 * This provides historic and forward values for a single {@link PriceIndex}, such as 'US-CPI-U'.
 * This is typically used in inflation products.
 */
public interface PriceIndexValues
    extends MarketDataView {

  /**
   * Obtains an instance from a curve and time-series of fixings.
   * <p>
   * The only supported implementation at present is {@link SimplePriceIndexValues}.
   * The curve must have x-values of {@linkplain ValueType#MONTHS months}.
   * The y-values must be {@linkplain ValueType#PRICE_INDEX price index values}.
   * The fixings time-series must not be empty.
   * 
   * @param index  the index
   * @param valuationDate  the valuation date for which the curve is valid
   * @param forwardCurve  the forward curve
   * @param fixings  the time-series of fixings
   * @return the price index values
   */
  public static PriceIndexValues of(
      PriceIndex index,
      LocalDate valuationDate,
      InterpolatedNodalCurve forwardCurve,
      LocalDateDoubleTimeSeries fixings) {

    return SimplePriceIndexValues.of(index, valuationDate, forwardCurve, fixings);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Price index.
   * <p>
   * The index that the rates are for.
   * 
   * @return the Price index
   */
  public abstract PriceIndex getIndex();

  /**
   * Gets the time-series of fixings for the index.
   * <p>
   * The time-series contains historic fixings of the index.
   * It may be empty if the data is not available.
   * 
   * @return the time-series fixings
   */
  public abstract LocalDateDoubleTimeSeries getFixings();

  /**
   * Gets the name of the underlying curve.
   * 
   * @return the underlying curve name
   */
  public abstract CurveName getCurveName();

  /**
   * Gets the number of parameters defining the curve.
   * <p>
   * If the curve has no parameters, zero must be returned.
   * 
   * @return the number of parameters
   */
  public abstract int getParameterCount();

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate at the specified fixing month.
   * <p>
   * The rate of the Price index, such as 'US-CPI-U', varies over time.
   * This method obtains the actual or estimated rate for the month.
   * <p>
   * This retrieves the actual rate if the fixing month is before the valuation month,
   * or the estimated rate if the fixing month is after the valuation month.
   * If the month equals the valuation month, then the best available value is returned.
   * 
   * @param observation  the rate observation, including the fixing month
   * @return the value of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double value(PriceIndexObservation observation);

  /**
   * Calculates the point sensitivity of the historic or forward value at the specified fixing month.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward value.
   * If a time-series was used, then there is no sensitivity.
   * The sensitivity refers to the result of {@link #value(PriceIndexObservation)}.
   * 
   * @param observation  the rate observation, including the fixing month
   * @return the point sensitivity of the value
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract PointSensitivityBuilder valuePointSensitivity(PriceIndexObservation observation);

  //-------------------------------------------------------------------------
  /**
   * Calculates the curve parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to curve parameter sensitivity.
   * The calculation typically involves multiplying the point and unit sensitivities.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract CurveCurrencyParameterSensitivities curveParameterSensitivity(InflationRateSensitivity pointSensitivity);

}
