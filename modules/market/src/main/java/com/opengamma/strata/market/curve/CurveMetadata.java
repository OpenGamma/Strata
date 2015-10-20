/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.List;
import java.util.Optional;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.market.value.ValueType;

/**
 * Metadata about a curve and curve parameters.
 * <p>
 * Implementations of this interface are used to store metadata about a curve.
 * For example, a curve may be defined based on financial instruments.
 * The parameters might represent 1 day, 1 week, 1 month, 3 month and 6 months.
 * The metadata could be used to describe each parameter in terms of a {@link Tenor}.
 * <p>
 * This metadata can be used by applications to interpret the parameters of the curve.
 * For example, the scenario framework uses the data when applying perturbations.
 * <p>
 * See {@link Curves} for helper methods that create common curve types.
 */
public interface CurveMetadata {

  /**
   * Gets the curve name.
   * 
   * @return the curve name
   */
  public abstract CurveName getCurveName();

  /**
   * Gets the x-value type, providing meaning to the x-values of the curve.
   * <p>
   * This type provides meaning to the x-values. For example, the x-value might
   * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
   * <p>
   * Note that if the x-value of the curve represents time as a year fraction, the day
   * count should be specified in the info map to define how the year fraction is calculated.
   * 
   * @return the x-value type
   */
  public abstract ValueType getXValueType();

  /**
   * Gets the y-value type, providing meaning to the y-values of the curve.
   * <p>
   * This type provides meaning to the y-values. For example, the y-value might
   * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
   * 
   * @return the y-value type
   */
  public abstract ValueType getYValueType();

  /**
   * Gets curve information of a specific type.
   * <p>
   * This method supplies whatever additional information is available for the curve.
   * <p>
   * The most common information is the {@linkplain CurveInfoType#DAY_COUNT day count}
   * and {@linkplain CurveInfoType#JACOBIAN curve calibration information}.
   * 
   * @param type  the type to find
   * @return the curve information
   * @throws IllegalArgumentException if the information is not found
   */
  public default <T> T getInfo(CurveInfoType<T> type) {
    return findInfo(type).orElseThrow(() -> new IllegalArgumentException(
        "Unable to find curve information of type " + type));
  }

  /**
   * Finds curve information of a specific type.
   * <p>
   * The most common information is the {@linkplain CurveInfoType#DAY_COUNT day count}
   * and {@linkplain CurveInfoType#JACOBIAN curve calibration information}.
   * <p>
   * If the info is not found, optional empty is returned.
   * 
   * @param type  the type to find
   * @return the curve information
   */
  public abstract <T> Optional<T> findInfo(CurveInfoType<T> type);

  /**
   * Gets metadata about each parameter underlying the curve, optional.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   * 
   * @return the parameter metadata
   */
  public abstract Optional<List<CurveParameterMetadata>> getParameterMetadata();

  /**
   * Returns an instance where the parameter metadata has been changed.
   * <p>
   * The result will contain the specified parameter metadata.
   * A null value is accepted and causes the result to have no parameter metadata.
   * 
   * @param parameterMetadata  the new parameter metadata
   * @return the new curve metadata
   */
  public abstract CurveMetadata withParameterMetadata(List<CurveParameterMetadata> parameterMetadata);

}
