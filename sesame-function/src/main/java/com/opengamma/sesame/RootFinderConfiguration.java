/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * Configuartion for a root finder.
 */
public class RootFinderConfiguration {

  private final double _rootFinderAbsoluteTolerance;
  private final double _rootFinderRelativeTolerance;
  private final int _rootFinderMaxIterations;

  public RootFinderConfiguration(double rootFinderAbsoluteTolerance,
                                 double rootFinderRelativeTolerance,
                                 int rootFinderMaxIterations) {
    _rootFinderAbsoluteTolerance = rootFinderAbsoluteTolerance;
    _rootFinderRelativeTolerance = rootFinderRelativeTolerance;
    _rootFinderMaxIterations = rootFinderMaxIterations;
  }

  //-------------------------------------------------------------------------
  public double getAbsoluteTolerance() {
    return _rootFinderAbsoluteTolerance;
  }

  public double getRelativeTolerance() {
    return _rootFinderRelativeTolerance;
  }

  public int getMaxIterations() {
    return _rootFinderMaxIterations;
  }

}
