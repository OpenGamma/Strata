/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to rates for an Ibor index.
 * <p>
 * This provides historic and forward rates for a single {@link IborIndex}, such as 'GBP-LIBOR-3M'.
 */
public interface IborIndexRates
    extends MarketDataView {

  /**
   * Obtains an instance from a forward curve, with an empty time-series of fixings.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must have x-values of {@linkplain ValueType#YEAR_FRACTION year fractions} with
   * the day count specified. The y-values must be {@linkplain ValueType#ZERO_RATE zero rates}
   * or {@linkplain ValueType#DISCOUNT_FACTOR discount factors}.
   * 
   * @param index  the index
   * @param valuationDate  the valuation date for which the curve is valid
   * @param forwardCurve  the forward curve
   * @return the rates view
   */
  public static IborIndexRates of(
      IborIndex index,
      LocalDate valuationDate,
      Curve forwardCurve) {

    return of(index, valuationDate, forwardCurve, LocalDateDoubleTimeSeries.empty());
  }

  /**
   * Obtains an instance from a curve and time-series of fixings.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must have x-values of {@linkplain ValueType#YEAR_FRACTION year fractions} with
   * the day count specified. The y-values must be {@linkplain ValueType#ZERO_RATE zero rates}
   * or {@linkplain ValueType#DISCOUNT_FACTOR discount factors}.
   * 
   * @param index  the index
   * @param valuationDate  the valuation date for which the curve is valid
   * @param forwardCurve  the forward curve
   * @param fixings  the time-series of fixings
   * @return the rates view
   */
  public static IborIndexRates of(
      IborIndex index,
      LocalDate valuationDate,
      Curve forwardCurve,
      LocalDateDoubleTimeSeries fixings) {
    if (forwardCurve.getMetadata().getYValueType().equals(ValueType.FORWARD_RATE)) {
      return SimpleIborIndexRates.of(index, valuationDate, forwardCurve, fixings);
    }
    DiscountFactors discountFactors = DiscountFactors.of(index.getCurrency(), valuationDate, forwardCurve);
    return DiscountIborIndexRates.of(index, discountFactors, fixings);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Ibor index.
   * <p>
   * The index that the rates are for.
   * 
   * @return the Ibor index
   */
  public abstract IborIndex getIndex();

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

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate at the specified fixing date.
   * <p>
   * The rate of the Ibor index, such as 'GBP-LIBOR-3M', varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * 
   * @param observation  the rate observation, including the fixing date
   * @return the rate of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rate(IborIndexObservation observation);

  /**
   * Ignores the time-series of fixings to get the forward rate at the specified
   * fixing date, used in rare and special cases. In most cases callers should use
   * {@link #rate(IborIndexObservation) rate(IborIndexObservation)}.
   * <p>
   * An instance of {@code IborIndexRates} is typically based on a forward curve and a historic time-series.
   * The {@code rate(LocalDate)} method uses either the curve or time-series, depending on whether the
   * fixing date is before or after the valuation date. This method only queries the forward curve,
   * totally ignoring the time-series, which is needed for rare and special cases only.
   * 
   * @param observation  the rate observation, including the fixing date
   * @return the rate of the index ignoring the time-series of fixings
   */
  public abstract double rateIgnoringFixings(IborIndexObservation observation);

  /**
   * Calculates the point sensitivity of the historic or forward rate at the specified fixing date.
   * <p>
   * This returns a sensitivity instance referring to the points that were queried in the market data.
   * If a time-series was used, then there is no sensitivity.
   * The sensitivity refers to the result of {@link #rate(IborIndexObservation) rate(IborIndexObservation)}.
   * 
   * @param observation  the rate observation, including the fixing date
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract PointSensitivityBuilder ratePointSensitivity(IborIndexObservation observation);

  /**
   * Ignores the time-series of fixings to get the forward rate point sensitivity at the
   * specified fixing date, used in rare and special cases. In most cases callers should use
   * {@link #ratePointSensitivity(IborIndexObservation) ratePointSensitivity(IborIndexObservation)}.
   * <p>
   * An instance of {@code IborIndexRates} is typically based on a forward curve and a historic time-series.
   * The {@code ratePointSensitivity(LocalDate)} method uses either the curve or time-series, depending on whether the
   * fixing date is before or after the valuation date. This method only queries the forward curve,
   * totally ignoring the time-series, which is needed for rare and special cases only.
   * 
   * @param observation  the rate observation, including the fixing date
   * @return the point sensitivity of the rate ignoring the time-series of fixings
   */
  public abstract PointSensitivityBuilder rateIgnoringFixingsPointSensitivity(IborIndexObservation observation);

  //-------------------------------------------------------------------------
  /**
   * Calculates the parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to parameter sensitivity.
   * The calculation typically involves multiplying the point and unit sensitivities.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract CurveCurrencyParameterSensitivities curveParameterSensitivity(IborRateSensitivity pointSensitivity);

}
