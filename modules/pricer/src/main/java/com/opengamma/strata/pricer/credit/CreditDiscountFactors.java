/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Provides access to discount factors for a single currency.
 * <p>
 * The discount factor represents the time value of money for the specified currency
 * when comparing the valuation date to the specified date.
 * <p>
 * This is also used for representing survival probabilities of a legal entity for a single currency.
 */
public interface CreditDiscountFactors
    extends MarketDataView, ParameterizedData {

  /**
   * Gets the currency.
   * <p>
   * The currency that discount factors are provided for.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Obtains day count convention.
   * <p>
   * This is typically the day count convention of the underlying curve.
   * 
   * @return the day count
   */
  public abstract DayCount getDayCount();

  /**
   * Creates an instance of {@link DiscountFactors}.
   * 
   * @return the instance
   */
  public abstract DiscountFactors toDiscountFactors();

  /**
   * Obtains the parameter keys of the underlying curve.
   * 
   * @return the parameter keys
   */
  public abstract DoubleArray getParameterKeys();

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a curve.
   * <p>
   * If the curve satisfies the conditions for ISDA compliant curve, 
   * {@code IsdaCompliantZeroRateDiscountFactors} is always instantiated. 
   * <p>
   * This must be updated once a new subclass is implemented.
   * 
   * @param currency  the currency
   * @param valuationDate  the valuation date for which the curve is valid
   * @param curve  the underlying curve
   * @return the discount factors view
   */
  public static CreditDiscountFactors of(Currency currency, LocalDate valuationDate, Curve curve) {
    CurveMetadata metadata = curve.getMetadata();
    if (metadata.getXValueType().equals(ValueType.YEAR_FRACTION) && metadata.getYValueType().equals(ValueType.ZERO_RATE)) {
      if (curve instanceof ConstantNodalCurve) {
        ConstantNodalCurve constantCurve = (ConstantNodalCurve) curve;
        return IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, constantCurve);
      }
      if (curve instanceof InterpolatedNodalCurve) {
        InterpolatedNodalCurve interpolatedCurve = (InterpolatedNodalCurve) curve;
        ArgChecker.isTrue(interpolatedCurve.getInterpolator().equals(CurveInterpolators.PRODUCT_LINEAR),
            "Interpolator must be PRODUCT_LINEAR");
        ArgChecker.isTrue(interpolatedCurve.getExtrapolatorLeft().equals(CurveExtrapolators.FLAT),
            "Left extrapolator must be FLAT");
        ArgChecker.isTrue(interpolatedCurve.getExtrapolatorRight().equals(CurveExtrapolators.PRODUCT_LINEAR),
            "Right extrapolator must be PRODUCT_LINEAR");
        return IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, interpolatedCurve);
      }
    }
    throw new IllegalArgumentException("Unknown curve type");
  }

  //-------------------------------------------------------------------------
  @Override
  public abstract CreditDiscountFactors withParameter(int parameterIndex, double newValue);

  @Override
  public abstract CreditDiscountFactors withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------
  /**
   * Checks if the instance is based on an ISDA compliant curve.
   * <p>
   * This returns 'false' by default, and should be overridden when needed.
   * 
   * @return true if this is an ISDA compliant curve, false otherwise
   */
  public default boolean isIsdaCompliant() {
    return false;
  }

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
  public double relativeYearFraction(LocalDate date);

  /**
   * Gets the discount factor for the specified date.
   * <p>
   * The discount factor represents the time value of money for the specified currency
   * when comparing the valuation date to the specified date.
   * <p>
   * If the valuation date is on the specified date, the discount factor is 1.
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
