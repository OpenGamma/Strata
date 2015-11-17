/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.MarketDataValue;
import com.opengamma.strata.market.Perturbation;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.key.OvernightIndexRatesKey;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to rates for an Overnight index.
 * <p>
 * This provides historic and forward rates for a single {@link OvernightIndex}, such as 'EUR-EONIA'.
 */
public interface OvernightIndexRates
    extends MarketDataValue<OvernightIndexRates> {

  /**
   * Gets the market data key.
   * <p>
   * This returns the {@link OvernightIndexRatesKey} that identifies this instance.
   * 
   * @return the market data key
   */
  @Override
  public default OvernightIndexRatesKey getKey() {
    return OvernightIndexRatesKey.of(getIndex());
  }

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
  public abstract LocalDateDoubleTimeSeries getTimeSeries();

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
   * The rate of the overnight index, such as 'EUR-EONIA', varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * The reference period for the underlying deposit is computed from the index conventions.
   * 
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rate(LocalDate fixingDate);

  /**
   * Calculates the point sensitivity of the historic or forward rate at the specified fixing date.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * If a time-series was used, then there is no sensitivity.
   * Otherwise, the sensitivity has the value 1.
   * The sensitivity refers to the result of {@link #rate(LocalDate)}.
   * 
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract PointSensitivityBuilder ratePointSensitivity(LocalDate fixingDate);

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
   * @param startDate  the start or effective date of the period on which the rate is computed
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the simply compounded rate associated to the period for the index
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double periodRate(LocalDate startDate, LocalDate endDate);

  /**
   * Calculates the point sensitivity of the historic or forward rate at the specified fixing period.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * The sensitivity refers to the result of {@link #periodRate(LocalDate, LocalDate)}.
   * 
   * @param startDate  the start or effective date of the period on which the rate is computed
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract PointSensitivityBuilder periodRatePointSensitivity(LocalDate startDate, LocalDate endDate);

  //-------------------------------------------------------------------------
  /**
   * Calculates the unit parameter sensitivity of the forward rate at the specified fixing date.
   * <p>
   * This returns the unit sensitivity to each parameter on the underlying curve at the specified date.
   * The sensitivity refers to the result of {@link #rate(LocalDate)}.
   * 
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the parameter sensitivity
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract CurveUnitParameterSensitivities unitParameterSensitivity(LocalDate fixingDate);

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
