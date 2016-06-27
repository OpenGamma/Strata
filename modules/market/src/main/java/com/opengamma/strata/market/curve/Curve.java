/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.Period;
import java.util.List;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A curve that maps a {@code double} x-value to a {@code double} y-value.
 * <p>
 * Implementations of this interface provide the ability to find a y-value on the curve from the x-value.
 * <p>
 * Each implementation will be backed by a number of <i>parameters</i>.
 * The meaning of the parameters is implementation dependent.
 * The sensitivity of the result to each of the parameters can also be obtained.
 * 
 * @see InterpolatedNodalCurve
 */
public interface Curve extends ParameterizedData {

  /**
   * Gets the curve metadata.
   * <p>
   * This method returns metadata about the curve and the curve parameters.
   * <p>
   * For example, a curve may be defined based on financial instruments.
   * The parameters might represent 1 day, 1 week, 1 month, 3 months, 6 months and 12 months.
   * The metadata could be used to describe each parameter in terms of a {@link Period}.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If parameter metadata is present, the size of the list will match the number of parameters of this curve.
   * 
   * @return the metadata
   */
  public abstract CurveMetadata getMetadata();

  /**
   * Returns a new curve with the specified metadata.
   * <p>
   * This allows the metadata of the curve to be changed while retaining all other information.
   * If parameter metadata is present, the size of the list must match the number of parameters of this curve.
   * 
   * @param metadata  the new metadata for the curve
   * @return the new curve
   */
  public abstract Curve withMetadata(CurveMetadata metadata);

  /**
   * Gets the curve name.
   * 
   * @return the curve name
   */
  public default CurveName getName() {
    return getMetadata().getCurveName();
  }

  @Override
  public default ParameterMetadata getParameterMetadata(int parameterIndex) {
    return getMetadata().getParameterMetadata(parameterIndex);
  }

  @Override
  public abstract Curve withParameter(int parameterIndex, double newValue);

  @Override
  default Curve withPerturbation(ParameterPerturbation perturbation) {
    return (Curve) ParameterizedData.super.withPerturbation(perturbation);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the y-value for the specified x-value.
   * 
   * @param x  the x-value to find the y-value for
   * @return the value at the x-value
   */
  public abstract double yValue(double x);

  /**
   * Computes the sensitivity of the y-value with respect to the curve parameters.
   * <p>
   * This returns an array with one element for each parameter of the curve.
   * The array contains the sensitivity of the y-value at the specified x-value to each parameter.
   * 
   * @param x  the x-value at which the parameter sensitivity is computed
   * @return the sensitivity
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract UnitParameterSensitivity yValueParameterSensitivity(double x);

  /**
   * Computes the first derivative of the curve.
   * <p>
   * The first derivative is {@code dy/dx}.
   * 
   * @param x  the x-value at which the derivative is taken
   * @return the first derivative
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract double firstDerivative(double x);

  //-------------------------------------------------------------------------
  /**
   * Creates a parameter sensitivity instance for this curve when the sensitivity values are known.
   * <p>
   * In most cases, {@link #yValueParameterSensitivity(double)} should be used and manipulated.
   * However, it can be useful to create a {@link UnitParameterSensitivity} from pre-computed sensitivity values.
   * 
   * @param sensitivities  the sensitivity values, which must match the parameter count of the curve
   * @return the sensitivity
   */
  public default UnitParameterSensitivity createParameterSensitivity(DoubleArray sensitivities) {
    List<ParameterMetadata> paramMeta = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
    return UnitParameterSensitivity.of(getName(), paramMeta, sensitivities);
  }

  /**
   * Creates a parameter sensitivity instance for this curve when the sensitivity values are known.
   * <p>
   * In most cases, {@link #yValueParameterSensitivity(double)} should be used and manipulated.
   * However, it can be useful to create a {@link CurrencyParameterSensitivity} from pre-computed sensitivity values.
   * 
   * @param currency  the currency
   * @param sensitivities  the sensitivity values, which must match the parameter count of the curve
   * @return the sensitivity
   */
  public default CurrencyParameterSensitivity createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    List<ParameterMetadata> paramMeta = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
    return CurrencyParameterSensitivity.of(getName(), paramMeta, currency, sensitivities);
  }

}
