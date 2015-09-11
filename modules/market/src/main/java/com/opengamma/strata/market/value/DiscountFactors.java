/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.Perturbation;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;

/**
 * Provides access to discount factors for a single currency.
 * <p>
 * The discount factor represents the time value of money for the specified currency
 * when comparing the valuation date to the specified date.
 */
public interface DiscountFactors {

  /**
   * Gets the currency.
   * <p>
   * The currency that discount factors are provided for.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

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
   * Gets the discount factor.
   * <p>
   * The discount factor represents the time value of money for the specified currency
   * when comparing the valuation date to the specified date.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param date  the date to discount to
   * @return the discount factor
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double discountFactor(LocalDate date);

  /**
   * Gets the discount factor with z-spread.
   * <p>
   * The discount factor represents the time value of money for the specified currency
   * when comparing the valuation date to the specified date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve. 
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param date  the date to discount to
   * @param zSpread  the z-spread
   * @param periodic  if true, the spread is added to periodic compounded rates,
   *  if false, the spread is added to continuously compounded rates
   * @param periodsPerYear  the number of periods per year
   * @return the discount factor
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double discountFactorWithSpread(LocalDate date, double zSpread, boolean periodic, int periodsPerYear);

  /**
   * Calculates the zero rate point sensitivity at the specified date.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity typically has the value {@code (-discountFactor * relativeYearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * 
   * @param date  the date to discount to
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivity(LocalDate date) {
    return zeroRatePointSensitivity(date, getCurrency());
  }

  /**
   * Calculates the zero rate point sensitivity with z-spread at the specified date.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity refers to the result of {@link #discountFactorWithSpread(LocalDate, double, boolean, int)}.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve. 
   * 
   * @param date  the date to discount to
   * @param zSpread  the z-spread
   * @param periodic  if true, the spread is added to periodic compounded rates,
   *  if false, the spread is added to continuously compounded rates
   * @param periodPerYear  the number of periods per year
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      LocalDate date,
      double zSpread,
      boolean periodic,
      int periodPerYear) {
    return zeroRatePointSensitivityWithSpread(date, getCurrency(), zSpread, periodic, periodPerYear);
  }

  /**
   * Calculates the zero rate point sensitivity at the specified date specifying the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity typically has the value {@code (-discountFactor * relativeYearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the curve.
   * 
   * @param date  the date to discount to
   * @param sensitivityCurrency  the currency of the sensitivity
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract ZeroRateSensitivity zeroRatePointSensitivity(LocalDate date, Currency sensitivityCurrency);

  /**
   * Calculates the zero rate point sensitivity with z-spread at the specified date specifying
   * the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity refers to the result of {@link #discountFactorWithSpread(LocalDate, double, boolean, int)}.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve. 
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the curve.
   * 
   * @param date  the date to discount to
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param zSpread  the z-spread
   * @param periodic  if true, the spread is added to periodic compounded rates,
   *  if false, the spread is added to continuously compounded rates
   * @param periodsPerYear  the number of periods per year
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      LocalDate date,
      Currency sensitivityCurrency,
      double zSpread,
      boolean periodic,
      int periodsPerYear);

  //-------------------------------------------------------------------------
  /**
   * Calculates the unit parameter sensitivity of the forward rate at the specified fixing date.
   * <p>
   * This returns the unit sensitivity to each parameter on the underlying curve at the specified date.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * 
   * @param date  the date to find the sensitivity for
   * @return the parameter sensitivity
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract CurveUnitParameterSensitivities unitParameterSensitivity(LocalDate date);

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
  public abstract CurveCurrencyParameterSensitivities curveParameterSensitivity(ZeroRateSensitivity pointSensitivity);

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
  public abstract DiscountFactors applyPerturbation(Perturbation<Curve> perturbation);

}
