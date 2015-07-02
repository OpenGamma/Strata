/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.List;
import java.util.Optional;

import org.joda.beans.ImmutableBean;

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
  public abstract Optional<List<CurveParameterMetadata>> getParameterMetadata();

}
