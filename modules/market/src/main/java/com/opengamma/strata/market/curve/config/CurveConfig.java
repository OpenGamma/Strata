/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.config;

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
}
