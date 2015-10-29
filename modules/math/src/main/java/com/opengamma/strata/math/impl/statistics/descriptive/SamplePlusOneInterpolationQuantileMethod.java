/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

/**
 * Implementation of a quantile estimator.
 * <p>
 * The quantile is linearly interpolated between two sample values.
 * The probability dimension on which the interpolation take place (X axis) is the ratio of the sample index and the
 * number of elements in the sample plus one ( <i>p<subscript>i</subscript> = i / (n+1)</i>). For each probability 
 * <i>p<subscript>i</subscript></i>, the distribution value is the sample value with same index. 
 * The index used above are the Java index plus 1.
 * <p> 
 * Reference: Value-At-Risk, OpenGamma Documentation 31, Version 0.1, April 2015.
 */
public final class SamplePlusOneInterpolationQuantileMethod
    extends InterpolationQuantileMethod {

  /** Default implementation. */
  public static final SamplePlusOneInterpolationQuantileMethod DEFAULT = new SamplePlusOneInterpolationQuantileMethod();

  @Override
  protected double indexCorrection() {
    return 0d;
  }

  @Override
  int sampleCorrection(int sampleSize) {
    return sampleSize + 1;
  }

}
