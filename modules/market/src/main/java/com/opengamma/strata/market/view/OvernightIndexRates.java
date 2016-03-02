/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.Perturbation;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to rates for an Overnight index.
 * <p>
 * This provides historic and forward rates for a single {@link OvernightIndex}, such as 'EUR-EONIA'.
 */
public interface OvernightIndexRates
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
  public static OvernightIndexRates of(
      OvernightIndex index,
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
  public static OvernightIndexRates of(
      OvernightIndex index,
      LocalDate valuationDate,
      Curve forwardCurve,
      LocalDateDoubleTimeSeries fixings) {

    DiscountFactors discountFactors = DiscountFactors.of(index.getCurrency(), valuationDate, forwardCurve);
    return DiscountOvernightIndexRates.of(index, discountFactors, fixings);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Overnight index.
   * <p>
   * The index that the rates are for.
   * 
   * @return the Overnight index
   */
  public abstract OvernightIndex getIndex();

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
   * The rate of the Overnight index, such as 'EUR-EONIA', varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * The reference period for the underlying deposit is computed from the index conventions.
   * 
   * @param observation  the rate observation, including the fixing date
   * @return the rate of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rate(OvernightIndexObservation observation);

  /**
   * Calculates the point sensitivity of the historic or forward rate at the specified fixing date.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * If a time-series was used, then there is no sensitivity.
   * Otherwise, the sensitivity has the value 1.
   * The sensitivity refers to the result of {@link #rate(OvernightIndexObservation)}.
   * 
   * @param observation  the rate observation, including the fixing date
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract PointSensitivityBuilder ratePointSensitivity(OvernightIndexObservation observation);

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate at the specified fixing period.
   * <p>
   * The start date should be on or after the valuation date. The end date should be after the start date.
   * <p>
   * This computes the forward rate in the simple simply compounded convention of the index between two given date.
   * This is used mainly to speed-up computation by computing the rate on a longer period instead of each individual 
   * overnight rate. When data related to the overnight index rate are stored based on the fixing date and not
   * the start and end date of the period, the call may return an {@code IllegalArgumentException}.
   * 
   * @param startDateObservation  the rate observation for the start of the period
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the simply compounded rate associated to the period for the index
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double periodRate(OvernightIndexObservation startDateObservation, LocalDate endDate);

  /**
   * Calculates the point sensitivity of the historic or forward rate at the specified fixing period.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * The sensitivity refers to the result of {@link #periodRate(OvernightIndexObservation, LocalDate)}.
   * 
   * @param startDateObservation  the rate observation for the start of the period
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract PointSensitivityBuilder periodRatePointSensitivity(
      OvernightIndexObservation startDateObservation,
      LocalDate endDate);

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
  public abstract CurveCurrencyParameterSensitivities curveParameterSensitivity(OvernightRateSensitivity pointSensitivity);

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
  public abstract OvernightIndexRates applyPerturbation(Perturbation<Curve> perturbation);

}
