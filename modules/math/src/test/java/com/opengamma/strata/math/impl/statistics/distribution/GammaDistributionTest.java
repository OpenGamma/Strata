/*
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
public class GammaDistributionTest extends ProbabilityDistributionTestCase {
  private static final double K = 1;
  private static final double THETA = 0.5;
  private static final GammaDistribution DIST = new GammaDistribution(K, THETA, ENGINE);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeK1() {
    new GammaDistribution(-1, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeK2() {
    new GammaDistribution(-1, 1, ENGINE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeTheta1() {
    new GammaDistribution(1, -1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeTheta2() {
    new GammaDistribution(1, -1, ENGINE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEngine() {
    new GammaDistribution(1, 1, null);
  }

  @Test
  public void test() {
    assertCDFWithNull(DIST);
    assertPDFWithNull(DIST);
    assertEquals(K, DIST.getK(), 0);
    assertEquals(THETA, DIST.getTheta(), 0);
    GammaDistribution other = new GammaDistribution(K, THETA, ENGINE);
    assertEquals(DIST, other);
    assertEquals(DIST.hashCode(), other.hashCode());
    other = new GammaDistribution(K, THETA);
    assertEquals(DIST, other);
    assertEquals(DIST.hashCode(), other.hashCode());
    other = new GammaDistribution(K + 1, THETA);
    assertFalse(other.equals(DIST));
    other = new GammaDistribution(K, THETA + 1);
    assertFalse(other.equals(DIST));
  }
}
