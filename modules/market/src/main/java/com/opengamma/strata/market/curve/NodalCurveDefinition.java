/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.LocalDate;
import java.time.Period;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;

/**
 * Provides the definition of how to calibrate a nodal curve.
 * <p>
 * A nodal curve is built from a number of parameters and described by metadata.
 * Calibration is based on a list of {@link CurveNode} instances, one for each parameter,
 * that specify the underlying instruments.
 */
public interface NodalCurveDefinition {

  /**
   * Gets the curve name.
   *
   * @return the curve name
   */
  public abstract CurveName getName();

  /**
   * Gets the number of parameters in the curve.
   * <p>
   * This returns the number of parameters in the curve, which equals the number of nodes.
   * 
   * @return the number of parameters
   */
  public default int getParameterCount() {
    return getNodes().size();
  }

  /**
   * Gets the y-value type, providing meaning to the y-values of the curve.
   * <p>
   * This type provides meaning to the y-values. For example, the y-value might
   * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
   * 
   * @return the y-value type
   */
  public ValueType getYValueType();

  /**
   * Gets the nodes that define the curve.
   * <p>
   * The nodes are used to calibrate the curve.
   * Each node is used to produce a parameter in the final curve.
   * 
   * @return the nodes
   */
  public abstract ImmutableList<CurveNode> getNodes();

  /**
   * Creates the curve metadata.
   * <p>
   * This method returns metadata about the curve and the curve parameters.
   * <p>
   * For example, a curve may be defined based on financial instruments.
   * The parameters might represent 1 day, 1 week, 1 month, 3 months, 6 months and 12 months.
   * The metadata could be used to describe each parameter in terms of a {@link Period}.
   * <p>
   * The optional parameter-level metadata will be populated on the resulting metadata.
   * The size of the parameter-level metadata will match the number of parameters of this curve.
   *
   * @param valuationDate  the valuation date
   * @param refData  the reference data
   * @return the metadata
   */
  public abstract CurveMetadata metadata(LocalDate valuationDate, ReferenceData refData);

  /**
   * Creates the curve from an array of parameter values.
   * <p>
   * The meaning of the parameters is determined by the implementation.
   * The size of the array must match the {@linkplain #getParameterCount() count of parameters}.
   * 
   * @param valuationDate  the valuation date
   * @param metadata  the curve metadata
   * @param parameters  the array of parameters
   * @return the curve
   */
  public abstract NodalCurve curve(LocalDate valuationDate, CurveMetadata metadata, DoubleArray parameters);

  /**
   * Converts this definition to the summary form.
   * <p>
   * The {@link CurveParameterSize} class provides a summary of this definition
   * consisting of the name and parameter size.
   * 
   * @return the summary form
   */
  public default CurveParameterSize toCurveParameterSize() {
    return CurveParameterSize.of(getName(), getParameterCount());
  }

}
