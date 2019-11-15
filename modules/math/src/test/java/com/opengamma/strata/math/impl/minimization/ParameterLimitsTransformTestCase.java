/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.opengamma.strata.math.impl.cern.MersenneTwister;
import com.opengamma.strata.math.impl.cern.MersenneTwister64;
import com.opengamma.strata.math.impl.cern.RandomEngine;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

/**
 * Abstract test.
 */
public abstract class ParameterLimitsTransformTestCase {

  protected static final RandomEngine RANDOM = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
  protected static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1, RANDOM);

  protected void assertRoundTrip(final ParameterLimitsTransform transform, final double modelParam) {
    final double fp = transform.transform(modelParam);
    final double mp = transform.inverseTransform(fp);
    assertThat(modelParam).isCloseTo(mp, offset(1e-8));
  }

  // reverse
  protected void assertReverseRoundTrip(final ParameterLimitsTransform transform, final double fitParam) {
    final double mp = transform.inverseTransform(fitParam);
    final double fp = transform.transform(mp);
    assertThat(fitParam).isCloseTo(fp, offset(1e-8));
  }

  protected void assertGradientRoundTrip(final ParameterLimitsTransform transform, final double modelParam) {
    final double g = transform.transformGradient(modelParam);
    final double fp = transform.transform(modelParam);
    final double gInv = transform.inverseTransformGradient(fp);
    assertThat(g).isCloseTo(1.0 / gInv, offset(1e-8));
  }

  protected void assertGradient(final ParameterLimitsTransform transform, final double modelParam) {
    final double eps = 1e-5;
    final double g = transform.transformGradient(modelParam);
    double fdg;
    try {
      final double down = transform.transform(modelParam - eps);
      final double up = transform.transform(modelParam + eps);
      fdg = (up - down) / 2 / eps;
    } catch (final IllegalArgumentException ex) {
      final double fp = transform.transform(modelParam);
      try {
        final double up = transform.transform(modelParam + eps);
        fdg = (up - fp) / eps;
      } catch (final IllegalArgumentException ex2) {
        final double down = transform.transform(modelParam - eps);
        fdg = (fp - down) / eps;
      }
    }
    assertThat(g).isCloseTo(fdg, offset(1e-6));
  }

  protected void assertInverseGradient(final ParameterLimitsTransform transform, final double fitParam) {
    final double eps = 1e-5;
    final double g = transform.inverseTransformGradient(fitParam);
    double fdg;

    final double down = transform.inverseTransform(fitParam - eps);
    final double up = transform.inverseTransform(fitParam + eps);
    fdg = (up - down) / 2 / eps;

    assertThat(g).isCloseTo(fdg, offset(1e-6));
  }

}
