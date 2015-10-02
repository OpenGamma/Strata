/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class NormalDistributionTest extends ProbabilityDistributionTestCase {
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1, ENGINE);
  private static final double[] X = new double[] {0, 0.1, 0.4, 0.8, 1, 1.32, 1.78, 2, 2.36, 2.88, 3, 3.5, 4, 4.5, 5 };
  private static final double[] P = new double[] {0.50000, 0.53982, 0.65542, 0.78814, 0.84134, 0.90658, 0.96246, 0.97724, 0.99086, 0.99801, 0.99865, 0.99976, 0.99996, 0.99999, 0.99999 };
  private static final double[] Z = new double[] {0.39894, 0.39695, 0.36827, 0.28969, 0.24197, 0.16693, 0.08182, 0.05399, 0.02463, 0.00630, 4.43184e-3, 8.72682e-4, 1.3383e-4, 1.59837e-5, 1.48671e-6 };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeSigmaDistribution() {
    new NormalDistribution(1, -0.4);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEngine() {
    new NormalDistribution(0, 1, null);
  }

  public void test() {
    assertCDF(P, X, NORMAL);
    assertPDF(Z, X, NORMAL);
    assertInverseCDF(X, NORMAL);
  }

  public void testRoundTrip() {
    int n = 29;
    for (int i = 0; i < n; i++) {
      double x = -7.0 + 0.5 * i;
      double p = NORMAL.getCDF(x);
      double xStar = NORMAL.getInverseCDF(p);
      assertEquals(x, xStar, 1e-5);
    }
  }

  public void testObject() {
    NormalDistribution other = new NormalDistribution(0, 1, ENGINE);
    assertEquals(NORMAL, other);
    assertEquals(NORMAL.hashCode(), other.hashCode());
    other = new NormalDistribution(0, 1);
    assertEquals(NORMAL, other);
    assertEquals(NORMAL.hashCode(), other.hashCode());
    other = new NormalDistribution(0.1, 1, ENGINE);
    assertFalse(NORMAL.equals(other));
    other = new NormalDistribution(0, 1.1, ENGINE);
    assertFalse(NORMAL.equals(other));
  }
}
