/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.Perturbation;
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
   * Gets the historic or forward rate at the specified fixing date.
   * <p>
   * The rate of the Ibor index, such as 'GBP-LIBOR-3M', varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * 
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rate(LocalDate fixingDate);

  /**
   * Ignores the time-series to get the forward rate at the specified fixing date, used in rare and special cases.
   * In most cases callers should use {@link IborIndexRates#rate(LocalDate) rate(LocalDate)}.
   * <p>
   * An instance of {@code IborIndexRates} is typically based on a forward curve and a historic time-series.
   * The {@code rate(LocalDate)} method uses either the curve or time-series, depending on whether the
   * fixing date is before or after the valuation date. This method only queries the forward curve,
   * totally ignoring the time-series, which is needed for rare and special cases only.
   * 
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index as given by the forward curve
   */
  public abstract double rateIgnoringTimeSeries(LocalDate fixingDate);

  /**
   * Calculates the point sensitivity of the historic or forward rate at the specified fixing date.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * If a time-series was used, then there is no sensitivity.
   * The sensitivity refers to the result of {@link #rate(LocalDate)}.
   * 
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract PointSensitivityBuilder ratePointSensitivity(LocalDate fixingDate);

  /**
   * Ignores the time-series to get the forward rate point sensitivity at the specified fixing date,
   * used in rare and special cases. In most cases callers should use
   * {@link IborIndexRates#ratePointSensitivity(LocalDate) ratePointSensitivity(LocalDate)}.
   * <p>
   * An instance of {@code IborIndexRates} is typically based on a forward curve and a historic time-series.
   * The {@code ratePointSensitivity(LocalDate)} method uses either the curve or time-series, depending on whether the
   * fixing date is before or after the valuation date. This method only queries the forward curve,
   * totally ignoring the time-series, which is needed for rare and special cases only.
   * 
   * @param fixingDate  the fixing date to query the rate for
   * @return the point sensitivity of the rate to the forward curve
   */
  public abstract PointSensitivityBuilder rateIgnoringTimeSeriesPointSensitivity(LocalDate fixingDate);

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
  public abstract CurveCurrencyParameterSensitivities curveParameterSensitivity(IborRateSensitivity pointSensitivity);

  //-------------------------------------------------------------------------
  /**
   * Applies the specified perturbation to the underlying curve.
   * <p>
   * This returns an instance where the curve that has been changed by the {@link Perturbation} instance.
   * 
   * @param perturbation  the perturbation to apply
   * @return the perturbed instance
   * @throws RuntimeException if the perturbation cannot be applied
   */
  public abstract IborIndexRates applyPerturbation(Perturbation<Curve> perturbation);

}
