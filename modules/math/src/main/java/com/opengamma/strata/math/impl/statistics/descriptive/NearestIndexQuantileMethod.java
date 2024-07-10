/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

/**
 * Implementation of a quantile estimator.
 * <p>
 * The estimation is one of the sorted sample data. Its index is given by the nearest (Math.round) integer to the
 * quantile multiplied by the size of the sample.
 * The Java index is the above index minus 1 (array index start at 0 and not 1).
 * <p>
 * Reference: Value-At-Risk, OpenGamma Documentation 31, Version 0.1, April 2015.
 */
public final class NearestIndexQuantileMethod
    extends DiscreteQuantileMethod {

  /** Default implementation. */
  public static final NearestIndexQuantileMethod DEFAULT = new NearestIndexQuantileMethod();

  @Override
  protected int index(double quantileSize) {
    return (int) Math.round(quantileSize);
  }

  @Override
  protected int sampleCorrection(int sampleSize) {
    return sampleSize;
  }

  @Override
  protected double indexShift() {
    return 0.5;
  }

}
