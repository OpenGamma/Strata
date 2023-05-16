/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.pricer.ZeroRatePeriodicDiscountFactors.EFFECTIVE_ZERO;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;

/**
 * Provides access to discount factors for a single currency.
 * <p>
 * The discount factor represents the time value of money for the specified currency
 * when comparing the valuation date to the specified date.
 */
public interface DiscountFactors
    extends MarketDataView, ParameterizedData {

  /**
   * Obtains an instance from a curve.
   * <p>
   * The curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curve must have x-values of {@linkplain ValueType#YEAR_FRACTION year fractions} with
   * the day count specified. The y-values must be {@linkplain ValueType#ZERO_RATE zero rates}
   * or {@linkplain ValueType#DISCOUNT_FACTOR discount factors}.
   * 
   * @param currency  the currency
   * @param valuationDate  the valuation date for which the curve is valid
   * @param curve  the underlying curve
   * @return the discount factors view
   */
  public static DiscountFactors of(Currency currency, LocalDate valuationDate, Curve curve) {
    if (curve.getMetadata().getYValueType().equals(ValueType.DISCOUNT_FACTOR)) {
      return SimpleDiscountFactors.of(currency, valuationDate, curve);
    }
    if (curve.getMetadata().getYValueType().equals(ValueType.ZERO_RATE)) {
      Optional<Integer> frequencyOpt = curve.getMetadata().findInfo(CurveInfoType.COMPOUNDING_PER_YEAR);
      if (frequencyOpt.isPresent()) {
        return ZeroRatePeriodicDiscountFactors.of(currency, valuationDate, curve);
      }
      return ZeroRateDiscountFactors.of(currency, valuationDate, curve);
    }
    throw new IllegalArgumentException(Messages.format(
        "Unknown value type in discount curve, must be 'DiscountFactor' or 'ZeroRate' but was '{}'",
        curve.getMetadata().getYValueType()));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * <p>
   * The currency that discount factors are provided for.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  //-------------------------------------------------------------------------
  @Override
  public abstract DiscountFactors withParameter(int parameterIndex, double newValue);

  @Override
  public abstract DiscountFactors withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------
  /**
   * Calculates the relative time between the valuation date and the specified date.
   * <p>
   * The {@code double} value returned from this method is used as the input to other methods.
   * It is typically calculated from a {@link DayCount}.
   * 
   * @param date  the date
   * @return  the year fraction
   * @throws RuntimeException if it is not possible to convert dates to relative times
   */
  public abstract double relativeYearFraction(LocalDate date);

  /**
   * Gets the discount factor for the specified date.
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
  public default double discountFactor(LocalDate date) {
    double yearFraction = relativeYearFraction(date);
    return discountFactor(yearFraction);
  }

  /**
   * Gets the discount factor for specified year fraction.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction 
   * @return the discount factor
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double discountFactor(double yearFraction);

  /**
   * Returns the discount factor derivative with respect to the year fraction or time.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction 
   * @return the discount factor derivative
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double discountFactorTimeDerivative(double yearFraction);

  /**
   * Gets the discount factor for the specified date with z-spread.
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
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the discount factor
   * @throws RuntimeException if the value cannot be obtained
   */
  public default double discountFactorWithSpread(
      LocalDate date,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double yearFraction = relativeYearFraction(date);
    return discountFactorWithSpread(yearFraction, zSpread, compoundedRateType, periodsPerYear);
  }

  /**
   * Gets the discount factor for the specified year fraction with z-spread.
   * <p>
   * The discount factor represents the time value of money for the specified currency
   * when comparing the valuation date to the specified date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction 
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the discount factor
   * @throws RuntimeException if the value cannot be obtained
   */
  public default double discountFactorWithSpread(
      double yearFraction,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (yearFraction < EFFECTIVE_ZERO) {
      return 1d;
    }
    double df = discountFactor(yearFraction);
    if (compoundedRateType.equals(CompoundedRateType.PERIODIC)) {
      ArgChecker.notNegativeOrZero(periodsPerYear, "periodPerYear");
      double ratePeriodicAnnualPlusOne =
          Math.pow(df, -1.0 / periodsPerYear / yearFraction) + zSpread / periodsPerYear;
      return Math.pow(ratePeriodicAnnualPlusOne, -periodsPerYear * yearFraction);
    } else {
      return df * Math.exp(-zSpread * yearFraction);
    }
  }

  /**
   * Gets the continuously compounded zero rate for the specified date.
   * <p>
   * The continuously compounded zero rate is coherent to {@link #discountFactor(LocalDate)} along with 
   * year fraction which is computed internally in each implementation.
   * 
   * @param date  the date to discount to
   * @return the zero rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public default double zeroRate(LocalDate date) {
    double yearFraction = relativeYearFraction(date);
    return zeroRate(yearFraction);
  }

  /**
   * Gets the continuously compounded zero rate for specified year fraction.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction 
   * @return the zero rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double zeroRate(double yearFraction);

  //-------------------------------------------------------------------------
  /**
   * Calculates the zero rate point sensitivity at the specified date.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-discountFactor * yearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * 
   * @param date  the date to discount to
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivity(LocalDate date) {
    double yearFraction = relativeYearFraction(date);
    return zeroRatePointSensitivity(yearFraction);
  }

  /**
   * Calculates the zero rate point sensitivity at the specified year fraction.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-discountFactor * yearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivity(double yearFraction) {
    return zeroRatePointSensitivity(yearFraction, getCurrency());
  }

  /**
   * Calculates the zero rate point sensitivity at the specified date specifying the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-discountFactor * yearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the market data.
   * 
   * @param date  the date to discount to
   * @param sensitivityCurrency  the currency of the sensitivity
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivity(LocalDate date, Currency sensitivityCurrency) {
    double yearFraction = relativeYearFraction(date);
    return zeroRatePointSensitivity(yearFraction, sensitivityCurrency);
  }

  /**
   * Calculates the zero rate point sensitivity at the specified year fraction specifying the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-discountFactor * yearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the market data.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction
   * @param sensitivityCurrency  the currency of the sensitivity
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract ZeroRateSensitivity zeroRatePointSensitivity(double yearFraction, Currency sensitivityCurrency);

  //-------------------------------------------------------------------------
  /**
   * Calculates the zero rate point sensitivity with z-spread at the specified date.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity refers to the result of {@link #discountFactorWithSpread(LocalDate, double, CompoundedRateType, int)}.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * 
   * @param date  the date to discount to
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      LocalDate date,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double yearFraction = relativeYearFraction(date);
    return zeroRatePointSensitivityWithSpread(yearFraction, zSpread, compoundedRateType, periodsPerYear);
  }

  /**
   * Calculates the zero rate point sensitivity with z-spread at the specified year fraction.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity refers to the result of {@link #discountFactorWithSpread(LocalDate, double, CompoundedRateType, int)}.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      double yearFraction,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return zeroRatePointSensitivityWithSpread(yearFraction, getCurrency(), zSpread, compoundedRateType, periodsPerYear);
  }

  /**
   * Calculates the zero rate point sensitivity with z-spread at the specified date specifying
   * the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity refers to the result of {@link #discountFactorWithSpread(LocalDate, double, CompoundedRateType, int)}.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the market data.
   * 
   * @param date  the date to discount to
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      LocalDate date,
      Currency sensitivityCurrency,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double yearFraction = relativeYearFraction(date);
    return zeroRatePointSensitivityWithSpread(yearFraction, sensitivityCurrency, zSpread, compoundedRateType, periodsPerYear);
  }

  /**
   * Calculates the zero rate point sensitivity with z-spread at the specified year fraction specifying
   * the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity refers to the result of {@link #discountFactorWithSpread(LocalDate, double, CompoundedRateType, int)}.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the market data.
   * <p>
   * The year fraction must be based on {@code #relativeYearFraction(LocalDate)}.
   * 
   * @param yearFraction  the year fraction
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public default ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      double yearFraction,
      Currency sensitivityCurrency,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (yearFraction <= EFFECTIVE_ZERO) {
      return ZeroRateSensitivity.of(getCurrency(), yearFraction, sensitivityCurrency, 0d);
    }
    ZeroRateSensitivity sensi = zeroRatePointSensitivity(yearFraction, sensitivityCurrency);
    double factor;
    if (compoundedRateType.equals(CompoundedRateType.PERIODIC)) {
      double df = discountFactor(yearFraction);
      double dfRoot = Math.pow(df, -1d / periodsPerYear / yearFraction);
      factor = dfRoot / df / Math.pow(dfRoot + zSpread / periodsPerYear, periodsPerYear * yearFraction + 1d);
    } else {
      factor = Math.exp(-zSpread * yearFraction);
    }
    return sensi.multipliedBy(factor);
  }

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
  public abstract CurrencyParameterSensitivities parameterSensitivity(ZeroRateSensitivity pointSensitivity);

  /**
   * Creates the parameter sensitivity when the sensitivity values are known.
   * <p>
   * In most cases, {@link #parameterSensitivity(ZeroRateSensitivity)} should be used and manipulated.
   * However, it can be useful to create parameter sensitivity from pre-computed sensitivity values.
   * <p>
   * There will typically be one {@link CurrencyParameterSensitivity} for each underlying data
   * structure, such as a curve. For example, if the discount factors are based on a single discount
   * curve, then there will be one {@code CurrencyParameterSensitivity} in the result.
   * 
   * @param currency  the currency
   * @param sensitivities  the sensitivity values, which must match the parameter count
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities);

}
