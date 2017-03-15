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
public class NonCentralChiSquaredDistributionTest {
  private static final double DOF = 3;
  private static final double NON_CENTRALITY = 1.5;
  private static final NonCentralChiSquaredDistribution DIST = new NonCentralChiSquaredDistribution(DOF, NON_CENTRALITY);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeDOF() {
    new NonCentralChiSquaredDistribution(-DOF, NON_CENTRALITY);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeNonCentrality() {
    new NonCentralChiSquaredDistribution(DOF, -NON_CENTRALITY);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullX() {
    DIST.getCDF(null);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testInverseCDF() {
    DIST.getInverseCDF(0.5);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testPDF() {
    DIST.getPDF(0.5);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandom() {
    DIST.nextRandom();
  }

  @Test
  public void test() {
    assertEquals(DIST.getDegrees(), DOF, 0);
    assertEquals(DIST.getNonCentrality(), NON_CENTRALITY, 0);
    assertEquals(DIST.getCDF(-100.), 0, 0);
    assertEquals(DIST.getCDF(0.), 0, 0);
    assertEquals(DIST.getCDF(5.), 0.649285, 1e-6);
  }

  @Test
  public void testObject() {
    assertEquals(DIST.getDegrees(), DOF, 0);
    assertEquals(DIST.getNonCentrality(), NON_CENTRALITY, 0);
    NonCentralChiSquaredDistribution other = new NonCentralChiSquaredDistribution(DOF, NON_CENTRALITY);
    assertEquals(DIST, other);
    assertEquals(DIST.hashCode(), other.hashCode());
    other = new NonCentralChiSquaredDistribution(DOF + 1, NON_CENTRALITY);
    assertFalse(other.equals(DIST));
    other = new NonCentralChiSquaredDistribution(DOF, NON_CENTRALITY + 1);
    assertFalse(other.equals(DIST));
  }

  /**
   * Numbers computed from R
   */
  @Test
  public void testLargeValues() {
    double x = 123;
    double dof = 6.4;
    double nonCent = 100.34;
    NonCentralChiSquaredDistribution dist = new NonCentralChiSquaredDistribution(dof, nonCent);
    assertEquals(0.7930769, dist.getCDF(x), 1e-6);

    x = 455.038;
    dof = 12;
    nonCent = 444.44;

    dist = new NonCentralChiSquaredDistribution(dof, nonCent);
    assertEquals(0.4961805, dist.getCDF(x), 1e-6);

    x = 999400;
    dof = 500;
    nonCent = 1000000;
    dist = new NonCentralChiSquaredDistribution(dof, nonCent);
    assertEquals(0.2913029, dist.getCDF(x), 1e-6);

  }

  /**
   * Numbers computed from R
   */
  @Test
  public void debugTest() {
    final double dof = 3.666;
    final double nonCentrality = 75;
    final double x = 13.89;

    final NonCentralChiSquaredDistribution chiSq1 = new NonCentralChiSquaredDistribution(dof, nonCentrality);
    final double y1 = Math.log(chiSq1.getCDF(x));
    assertEquals(-15.92129, y1, 1e-5);
  }

}
