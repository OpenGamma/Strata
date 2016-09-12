/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * The sample Fisher kurtosis gives a measure of how heavy the tails of a distribution are
 * with respect to the normal distribution (which has a Fisher kurtosis of zero).
 * An estimator of the kurtosis is
 * $$
 * \begin{align*}
 * \mu_4 = \frac{(n+1)n}{(n-1)(n-2)(n-3)}\frac{\sum_{i=1}^n (x_i - \overline{x})^4}{\mu_2^2} - 3\frac{(n-1)^2}{(n-2)(n-3)}
 * \end{align*}
 * $$
 * where $\overline{x}$ is the sample mean and $\mu_2$ is the unbiased estimator of the population variance.
 * <p>
 * Fisher kurtosis is also known as the _excess kurtosis_.
 */
public class SampleFisherKurtosisCalculator implements Function<double[], Double> {

  private static final Function<double[], Double> MEAN = new MeanCalculator();

  @Override
  public Double apply(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length >= 4, "Need at least four points to calculate kurtosis");
    double sum = 0;
    double mean = MEAN.apply(x);
    double variance = 0;
    for (Double d : x) {
      double diff = d - mean;
      double diffSq = diff * diff;
      variance += diffSq;
      sum += diffSq * diffSq;
    }
    int n = x.length;
    double n1 = n - 1;
    double n2 = n1 - 1;
    variance /= n1;
    return n * (n + 1.) * sum / (n1 * n2 * (n - 3.) * variance * variance) - 3 * n1 * n1 / (n2 * (n - 3.));
  }

}
