/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Calculates the geometric mean of a series of data. 
 * <p>
 * The geometric mean $\mu$ of a series of elements $x_1, x_2, \dots, x_n$ is given by:
 * $$
 * \begin{align*}
 * \mu = \left({\prod\limits_{i=1}^n x_i}\right)^{\frac{1}{n}}
 * \end{align*}
 * $$
 * 
 */
public class GeometricMeanCalculator implements Function<double[], Double> {

  /**
   * @param x The array of data, not null or empty
   * @return The geometric mean
   */
  @Override
  public Double apply(double[] x) {
    ArgChecker.notEmpty(x, "x");
    int n = x.length;
    double mult = x[0];
    for (int i = 1; i < n; i++) {
      mult *= x[i];
    }
    return Math.pow(mult, 1. / n);
  }

}
