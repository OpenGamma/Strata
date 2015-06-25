/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.List;
import java.util.Optional;

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;

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
 */
public interface CurveMetadata
    extends ImmutableBean {

  /**
   * Creates a metadata instance without parameter information.
   * <p>
   * The resulting metadata will have no parameter metadata.
   * For more control, see {@link DefaultCurveMetadata}.
   * 
   * @param name  the curve name
   * @return the metadata
   */
  public static CurveMetadata of(String name) {
    return of(CurveName.of(name));
  }

  /**
   * Creates a metadata instance without parameter information.
   * <p>
   * The resulting metadata will have no parameter metadata.
   * For more control, see {@link DefaultCurveMetadata}.
   * 
   * @param name  the curve name
   * @return the metadata
   */
  public static CurveMetadata of(CurveName name) {
    return DefaultCurveMetadata.of(name);
  }

  /**
   * Creates a metadata instance with parameter information.
   * <p>
   * The parameter metadata must match the number of parameters on the curve.
   * An empty list is accepted and interpreted as meaning that no parameter metadata is present.
   * For more control, see {@link DefaultCurveMetadata}.
   * 
   * @param name  the curve name
   * @param parameters  the parameter metadata
   * @return the metadata
   */
  public static CurveMetadata of(String name, List<? extends CurveParameterMetadata> parameters) {
    return of(CurveName.of(name), parameters);
  }

  /**
   * Creates a metadata instance with parameter information.
   * <p>
   * The parameter metadata must match the number of parameters on the curve.
   * An empty list is accepted and interpreted as meaning that no parameter metadata is present.
   * For more control, see {@link DefaultCurveMetadata}.
   * 
   * @param name  the curve name
   * @param parameters  the parameter metadata
   * @return the metadata
   */
  public static CurveMetadata of(CurveName name, List<? extends CurveParameterMetadata> parameters) {
    return DefaultCurveMetadata.of(name, ImmutableList.copyOf(parameters));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the curve name.
   * 
   * @return the curve name
   */
  public abstract CurveName getCurveName();

  /**
   * Gets the day count, optional.
   * <p>
   * If the x-value of the curve represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   * 
   * @return the day count
   */
  public abstract Optional<DayCount> getDayCount();

  /**
   * Gets metadata about each parameter underlying the curve, optional.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   * 
   * @return the parameter metadata
   */
  public abstract Optional<List<CurveParameterMetadata>> getParameters();

}
