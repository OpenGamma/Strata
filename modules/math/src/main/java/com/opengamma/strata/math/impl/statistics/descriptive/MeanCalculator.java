/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Calculates the arithmetic mean of a series of data.
 * <p>
 * The arithmetic mean $\mu$ of a series of elements $x_1, x_2, \dots, x_n$ is given by:
 * $$
 * \begin{align*}
 * \mu = \frac{1}{n}\left({\sum\limits_{i=1}^n x_i}\right)
 * \end{align*}
 * $$
 */
public class MeanCalculator implements Function<double[], Double> {

  @Override
  public Double apply(double[] x) {
    ArgChecker.notEmpty(x, "x");
    if (x.length == 1) {
      return x[0];
    }
    double sum = 0;
    for (Double d : x) {
      sum += d;
    }
    return sum / x.length;
  }

}
