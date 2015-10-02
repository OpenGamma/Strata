/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Calculates the population variance of a series of data.
 * <p>
 * The unbiased population variance $\mathrm{var}$ of a series $x_1, x_2, \dots, x_n$ is given by:
 * $$
 * \begin{align*}
 * \text{var} = \frac{1}{n}\sum_{i=1}^{n}(x_i - \overline{x})^2
 * \end{align*}
 * $$
 * where $\overline{x}$ is the sample mean. For the sample variance, see {@link SampleVarianceCalculator}.
 */
public class PopulationVarianceCalculator extends Function1D<double[], Double> {
  private final Function1D<double[], Double> _variance = new SampleVarianceCalculator();

  /**
   * @param x The array of data, not null, must contain at least two elements
   * @return The population variance
   */
  @Override
  public Double evaluate(final double[] x) {
    ArgChecker.notNull(x, "x");
    final int n = x.length;
    ArgChecker.isTrue(n >= 2, "Need at least two points to calculate the population variance");
    return _variance.evaluate(x) * (n - 1) / n;
  }
}
