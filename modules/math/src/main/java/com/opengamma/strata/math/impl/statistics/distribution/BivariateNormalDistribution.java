/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import com.opengamma.strata.collect.ArgChecker;

/**
 * The bivariate normal distribution is a continuous probability distribution
 * of two variables, $x$ and $y$, with cdf
 * $$
 * \begin{align*}
 * M(x, y, \rho) = \frac{1}{2\pi\sqrt{1 - \rho^2}}\int_{-\infty}^x\int_{-\infty}^{y} e^{\frac{-(X^2 - 2\rho XY + Y^2)}{2(1 - \rho^2)}} dX dY
 * \end{align*}
 * $$
 * where $\rho$ is the correlation between $x$ and $y$.
 * <p>
 * The implementation of the cdf is taken from "Better Approximations to Cumulative Normal Functions", West
 * (<a href="http://www.codeplanet.eu/files/download/accuratecumnorm.pdf">link</a>).
 */
public class BivariateNormalDistribution implements ProbabilityDistribution<double[]> {

  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  private static final double TWO_PI = 2 * Math.PI;
  private static final double[] X = new double[] {0.04691008, 0.23076534, 0.5, 0.76923466, 0.95308992};
  private static final double[] Y = new double[] {0.018854042, 0.038088059, 0.0452707394, 0.038088059, 0.018854042};

  /**
   * @param x The parameters for the function, $(x, y, \rho$, with $-1 \geq \rho \geq 1$, not null 
   * @return The cdf
   */
  @Override
  public double getCDF(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length == 3, "Need a, b and rho values");
    ArgChecker.isTrue(x[2] >= -1 && x[2] <= 1, "Correlation must be >= -1 and <= 1");
    double a = x[0];
    double b = x[1];
    double rho = x[2];
    if (a == Double.POSITIVE_INFINITY || b == Double.POSITIVE_INFINITY) {
      return 1;
    }
    if (a == Double.NEGATIVE_INFINITY || b == Double.NEGATIVE_INFINITY) {
      return 0;
    }
    double sumSq = (a * a + b * b) / 2.;
    double rho1, rho2, rho3, ab, absDiff, h5, c, d, mult = 0, rho3Sq, eab, e, result;
    if (Math.abs(rho) >= 0.7) {
      rho1 = 1 - rho * rho;
      rho2 = Math.sqrt(rho1);
      if (rho < 0) {
        b *= -1;
      }
      ab = a * b;
      eab = Math.exp(-ab / 2.);
      if (Math.abs(rho) < 1) {
        absDiff = Math.abs(a - b);
        h5 = absDiff * absDiff / 2.;
        absDiff = absDiff / rho2;
        c = 0.5 - ab / 8.;
        d = 3. - 2. * c * h5;
        mult = 0.13298076 * absDiff * d * (1 - NORMAL.getCDF(absDiff)) - Math.exp(-h5 / rho1) * (d + c * rho1) * 0.053051647;
        for (int i = 0; i < 5; i++) {
          rho3 = rho2 * X[i];
          rho3Sq = rho3 * rho3;
          rho1 = Math.sqrt(1 - rho3Sq);
          if (eab == 0) {
            e = 0;
          } else {
            e = Math.exp(-ab / (1 + rho1)) / rho1 / eab;
          }
          mult = mult - Y[i] * Math.exp(-h5 / rho3Sq) * (e - 1 - c * rho3Sq);
        }
      }
      double corr = Double.isNaN(mult) ? 0. : mult * rho2 * eab;
      result = corr + NORMAL.getCDF(Math.min(a, b));
      if (rho < 0) {
        result = NORMAL.getCDF(a) - result;
      }
      return result;
    }
    ab = a * b;
    if (rho != 0) {
      for (int i = 0; i < 5; i++) {
        rho3 = rho * X[i];
        rho1 = 1 - rho3 * rho3;
        mult = mult + Y[i] * Math.exp((rho3 * ab - sumSq) / rho1) / Math.sqrt(rho1);
      }
    }
    double corr = Double.isNaN(mult) ? 0. : rho * mult;
    return NORMAL.getCDF(a) * NORMAL.getCDF(b) + corr;
  }

  /**
   * {@inheritDoc}
   * @return Not supported
   * @throws UnsupportedOperationException always
   */
  @Override
  public double getInverseCDF(double[] p) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param x The parameters for the function, $(x, y, \rho$, with $-1 \geq \rho \geq 1$, not null
   * @return The pdf  
   */
  @Override
  public double getPDF(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length == 3, "Need a, b and rho values");
    ArgChecker.isTrue(x[2] >= -1 && x[2] <= 1, "Correlation must be >= -1 and <= 1");
    double denom = 1 - x[2] * x[2];
    return Math.exp(-(x[0] * x[0] - 2 * x[2] * x[0] * x[1] + x[1] * x[1]) / (2 * denom)) / (TWO_PI * Math.sqrt(denom));
  }

  /**
   * {@inheritDoc}
   * @return Not supported
   * @throws UnsupportedOperationException always
   */
  @Override
  public double nextRandom() {
    throw new UnsupportedOperationException();
  }

}
