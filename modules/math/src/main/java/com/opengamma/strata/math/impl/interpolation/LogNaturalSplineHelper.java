/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

/**
 * 
 */
public class LogNaturalSplineHelper extends NaturalSplineInterpolator {

  /**
   * In contrast with the original natural spline, the tridiagonal algorithm is used by passing
   * {@link LogCubicSplineNaturalSolver}. Note that the data are NOT log-scaled at this stage.
   */
  public LogNaturalSplineHelper() {
    super(new LogCubicSplineNaturalSolver());
  }

}
