/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Calculates the sample standard deviation of a series of data. The sample standard deviation of a series of data is defined as the square root of 
 * the sample variance (see {@link SampleVarianceCalculator}).
 */
public class SampleStandardDeviationCalculator implements Function<double[], Double> {

  private static final Function<double[], Double> VARIANCE = new SampleVarianceCalculator();

  @Override
  public Double apply(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length >= 2, "Need at least two points to calculate standard deviation");
    return Math.sqrt(VARIANCE.apply(x));
  }

}
