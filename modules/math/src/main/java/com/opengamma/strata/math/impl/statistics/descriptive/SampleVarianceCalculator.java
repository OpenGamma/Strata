/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Calculates the sample variance of a series of data. 
 * <p> 
 * The unbiased sample variance $\mathrm{var}$ of a series $x_1, x_2, \dots, x_n$ is given by:
 * $$
 * \begin{align*}
 * \text{var} = \frac{1}{n-1}\sum_{i=1}^{n}(x_i - \overline{x})^2
 * \end{align*}
 * $$
 * where $\overline{x}$ is the sample mean. For the population variance, see {@link PopulationVarianceCalculator}.
 */
public class SampleVarianceCalculator implements Function<double[], Double> {

  private static final Function<double[], Double> MEAN = new MeanCalculator();

  @Override
  public Double apply(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length >= 2, "Need at least two points to calculate the sample variance");
    Double mean = MEAN.apply(x);
    double sum = 0;
    for (Double value : x) {
      double diff = value - mean;
      sum += diff * diff;
    }
    int n = x.length;
    return sum / (n - 1);
  }

}
