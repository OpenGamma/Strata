/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.Assert;
import org.testng.annotations.Test;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

/**
 * Abstract test.
 */
@Test
public abstract class ProbabilityDistributionTestCase {

  protected static final double EPS = 1e-5;
  protected static final RandomEngine ENGINE = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);

  protected void assertCDF(final double[] p, final double[] x, final ProbabilityDistribution<Double> dist) {
    assertCDFWithNull(dist);
    for (int i = 0; i < p.length; i++) {
      assertEquals(dist.getCDF(x[i]), p[i], EPS);
    }
  }

  protected void assertPDF(final double[] z, final double[] x, final ProbabilityDistribution<Double> dist) {
    assertPDFWithNull(dist);
    for (int i = 0; i < z.length; i++) {
      assertEquals(dist.getPDF(x[i]), z[i], EPS);
    }
  }

  protected void assertInverseCDF(final double[] x, final ProbabilityDistribution<Double> dist) {
    assertInverseCDFWithNull(dist);
    for (final double d : x) {
      assertEquals(dist.getInverseCDF(dist.getCDF(d)), d, EPS);
    }
    try {
      dist.getInverseCDF(3.4);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    try {
      dist.getInverseCDF(-0.2);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }

  protected void assertInverseCDFWithNull(final ProbabilityDistribution<Double> dist) {
    try {
      dist.getInverseCDF(null);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }

  protected void assertPDFWithNull(final ProbabilityDistribution<Double> dist) {
    try {
      dist.getPDF(null);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }

  protected void assertCDFWithNull(final ProbabilityDistribution<Double> dist) {
    try {
      dist.getCDF(null);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }
}
