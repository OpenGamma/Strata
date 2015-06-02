/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.config;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Configuration specifying how to calibrate a curve.
 * <p>
 * This class contains a list of {@link CurveNode} instances specifying the instruments which make up the curve.
 */
public interface CurveConfig {

  /**
   * Returns the curve name.
   *
   * @return the curve name
   */
  public abstract CurveName getName();

  /**
   * Returns the curve metadata.
   * <p>
   * This method returns metadata about the curve and the curve parameters.
   * <p>
   * For example, a curve may be defined based on financial instruments.
   * The parameters might represent 1 day, 1 week, 1 month, 3 months, 6 months and 12 months.
   * The metadata could be used to describe each parameter in terms of a {@link Period}.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If parameter metadata is not known, the list will be empty.
   * Otherwise, the size of the parameter metadata list will match the number of parameters of this curve.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @return the metadata
   */
  public abstract CurveMetadata metadata(LocalDate valuationDate);
}
