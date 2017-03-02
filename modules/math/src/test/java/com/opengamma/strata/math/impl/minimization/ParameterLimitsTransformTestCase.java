/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

/**
 * Abstract test.
 */
@Test
public abstract class ParameterLimitsTransformTestCase {

  protected static final RandomEngine RANDOM = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
  protected static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1, RANDOM);

  protected void assertRoundTrip(final ParameterLimitsTransform transform, final double modelParam) {
    final double fp = transform.transform(modelParam);
    final double mp = transform.inverseTransform(fp);
    assertEquals(modelParam, mp, 1e-8);
  }

  // reverse
  protected void assertReverseRoundTrip(final ParameterLimitsTransform transform, final double fitParam) {
    final double mp = transform.inverseTransform(fitParam);
    final double fp = transform.transform(mp);
    assertEquals(fitParam, fp, 1e-8);
  }

  protected void assertGradientRoundTrip(final ParameterLimitsTransform transform, final double modelParam) {
    final double g = transform.transformGradient(modelParam);
    final double fp = transform.transform(modelParam);
    final double gInv = transform.inverseTransformGradient(fp);
    assertEquals(g, 1.0 / gInv, 1e-8);
  }

  protected void assertGradient(final ParameterLimitsTransform transform, final double modelParam) {
    final double eps = 1e-5;
    final double g = transform.transformGradient(modelParam);
    double fdg;
    try {
      final double down = transform.transform(modelParam - eps);
      final double up = transform.transform(modelParam + eps);
      fdg = (up - down) / 2 / eps;
    } catch (final IllegalArgumentException e) {
      final double fp = transform.transform(modelParam);
      try {
        final double up = transform.transform(modelParam + eps);
        fdg = (up - fp) / eps;
      } catch (final IllegalArgumentException e2) {
        final double down = transform.transform(modelParam - eps);
        fdg = (fp - down) / eps;
      }
    }
    assertEquals(g, fdg, 1e-6);
  }

  protected void assertInverseGradient(final ParameterLimitsTransform transform, final double fitParam) {
    final double eps = 1e-5;
    final double g = transform.inverseTransformGradient(fitParam);
    double fdg;

    final double down = transform.inverseTransform(fitParam - eps);
    final double up = transform.inverseTransform(fitParam + eps);
    fdg = (up - down) / 2 / eps;

    assertEquals(g, fdg, 1e-6);
  }

}
