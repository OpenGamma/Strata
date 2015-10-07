/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.Arrays;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Calculates the median of a series of data.
 * <p>
 * If the data are sorted from lowest to highest $(x_1, x_2, \dots, x_n)$, the median is given by
 * $$
 * \begin{align*}
 * m =
 * \begin{cases}
 * x_{\frac{n+1}{2}}\quad & n \text{ odd}\\
 * \frac{1}{2}\left(x_{\frac{n}{2}} + x_{\frac{n}{2} + 1}\right)\quad & n \text{ even}
 * \end{cases} 
 * \end{align*}
 * $$
 */
public class MedianCalculator implements Function<double[], Double> {

  @Override
  public Double apply(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length > 0, "x cannot be empty");
    if (x.length == 1) {
      return x[0];
    }
    double[] x1 = Arrays.copyOf(x, x.length);
    Arrays.sort(x1);
    int mid = x1.length / 2;
    if (x1.length % 2 == 1) {
      return x1[mid];
    }
    return (x1[mid] + x1[mid - 1]) / 2.;
  }

}
