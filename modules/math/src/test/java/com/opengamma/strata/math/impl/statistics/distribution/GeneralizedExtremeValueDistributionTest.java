/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class GeneralizedExtremeValueDistributionTest extends ProbabilityDistributionTestCase {
  private static final double MU = 1.5;
  private static final double SIGMA = 0.6;
  private static final double KSI = 0.7;
  private static final GeneralizedExtremeValueDistribution DIST = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI);
  private static final double LARGE_X = 1e10;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadConstructor() {
    new GeneralizedExtremeValueDistribution(MU, -SIGMA, KSI);
  }

  @Test
  public void testBadInputs() {
    assertCDFWithNull(DIST);
    assertPDFWithNull(DIST);
  }

  @Test
  public void testObject() {
    assertEquals(MU, DIST.getMu(), 0);
    assertEquals(SIGMA, DIST.getSigma(), 0);
    assertEquals(KSI, DIST.getKsi(), 0);
    GeneralizedExtremeValueDistribution other = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI);
    assertEquals(DIST, other);
    assertEquals(DIST.hashCode(), other.hashCode());
    other = new GeneralizedExtremeValueDistribution(MU + 1, SIGMA, KSI);
    assertFalse(other.equals(DIST));
    other = new GeneralizedExtremeValueDistribution(MU, SIGMA + 1, KSI);
    assertFalse(other.equals(DIST));
    other = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI + 1);
    assertFalse(other.equals(DIST));
  }

  @Test
  public void testSupport() {
    ProbabilityDistribution<Double> dist = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI);
    double limit = MU - SIGMA / KSI;
    assertLimit(dist, limit - EPS);
    assertEquals(dist.getCDF(limit + EPS), 0, EPS);
    assertEquals(dist.getCDF(LARGE_X), 1, EPS);
    dist = new GeneralizedExtremeValueDistribution(MU, SIGMA, -KSI);
    limit = MU + SIGMA / KSI;
    assertLimit(dist, limit + EPS);
    assertEquals(dist.getCDF(-LARGE_X), 0, EPS);
    assertEquals(dist.getCDF(limit - EPS), 1, EPS);
    dist = new GeneralizedExtremeValueDistribution(MU, SIGMA, 0);
    assertEquals(dist.getCDF(-LARGE_X), 0, EPS);
    assertEquals(dist.getCDF(LARGE_X), 1, EPS);
  }

  private void assertLimit(final ProbabilityDistribution<Double> dist, final double limit) {
    try {
      dist.getCDF(limit);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    try {
      dist.getPDF(limit);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }
}
