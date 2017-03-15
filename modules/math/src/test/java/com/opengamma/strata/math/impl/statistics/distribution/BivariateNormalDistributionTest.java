/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class BivariateNormalDistributionTest {
  private static final ProbabilityDistribution<double[]> DIST = new BivariateNormalDistribution();
  private static final double EPS = 1e-8;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullCDF() {
    DIST.getCDF(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInsufficientLengthCDF() {
    DIST.getCDF(new double[] {2, 1 });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testExcessiveLengthCDF() {
    DIST.getCDF(new double[] {2, 1, 4, 5 });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighCorrelation() {
    DIST.getCDF(new double[] {1., 1., 3. });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowCorrelation() {
    DIST.getCDF(new double[] {1., 1., -3. });
  }

  @Test
  public void test() {
    assertEquals(DIST.getCDF(new double[] {Double.POSITIVE_INFINITY, Math.random(), Math.random() }), 1, 0);
    assertEquals(DIST.getCDF(new double[] {Math.random(), Double.POSITIVE_INFINITY, Math.random() }), 1, 0);
    assertEquals(DIST.getCDF(new double[] {Double.NEGATIVE_INFINITY, Math.random(), Math.random() }), 0, 0);
    assertEquals(DIST.getCDF(new double[] {Math.random(), Double.NEGATIVE_INFINITY, Math.random() }), 0, 0);
    assertEquals(DIST.getCDF(new double[] {0.0, 0.0, 0.0 }), 0.25, EPS);
    assertEquals(DIST.getCDF(new double[] {0.0, 0.0, -0.5 }), 1. / 6, EPS);
    assertEquals(DIST.getCDF(new double[] {0.0, 0.0, 0.5 }), 1. / 3, EPS);

    assertEquals(DIST.getCDF(new double[] {0.0, -0.5, 0.0 }), 0.1542687694, EPS);
    assertEquals(DIST.getCDF(new double[] {0.0, -0.5, -0.5 }), 0.0816597607, EPS);
    assertEquals(DIST.getCDF(new double[] {0.0, -0.5, 0.5 }), 0.2268777781, EPS);

    assertEquals(DIST.getCDF(new double[] {0.0, 0.5, 0.0 }), 0.3457312306, EPS);
    assertEquals(DIST.getCDF(new double[] {0.0, 0.5, -0.5 }), 0.2731222219, EPS);
    assertEquals(DIST.getCDF(new double[] {0.0, 0.5, 0.5 }), 0.4183402393, EPS);

    assertEquals(DIST.getCDF(new double[] {-0.5, 0.0, 0.0 }), 0.1542687694, EPS);
    assertEquals(DIST.getCDF(new double[] {-0.5, 0.0, -0.5 }), 0.0816597607, EPS);
    assertEquals(DIST.getCDF(new double[] {-0.5, 0.0, 0.5 }), 0.2268777781, EPS);

    assertEquals(DIST.getCDF(new double[] {-0.5, -0.5, 0.0 }), 0.0951954128, EPS);
    assertEquals(DIST.getCDF(new double[] {-0.5, -0.5, -0.5 }), 0.0362981865, EPS);
    assertEquals(DIST.getCDF(new double[] {-0.5, -0.5, 0.5 }), 0.1633195213, EPS);

    assertEquals(DIST.getCDF(new double[] {-0.5, 0.5, 0.0 }), 0.2133421259, EPS);
    assertEquals(DIST.getCDF(new double[] {-0.5, 0.5, -0.5 }), 0.1452180174, EPS);
    assertEquals(DIST.getCDF(new double[] {-0.5, 0.5, 0.5 }), 0.2722393522, EPS);

    assertEquals(DIST.getCDF(new double[] {0.5, 0.0, 0.0 }), 0.3457312306, EPS);
    assertEquals(DIST.getCDF(new double[] {0.5, 0.0, -0.5 }), 0.2731222219, EPS);
    assertEquals(DIST.getCDF(new double[] {0.5, 0.0, 0.5 }), 0.4183402393, EPS);

    assertEquals(DIST.getCDF(new double[] {0.5, -0.5, 0.0 }), 0.2133421259, EPS);
    assertEquals(DIST.getCDF(new double[] {0.5, -0.5, -0.5 }), 0.1452180174, EPS);
    assertEquals(DIST.getCDF(new double[] {0.5, -0.5, 0.5 }), 0.2722393522, EPS);

    assertEquals(DIST.getCDF(new double[] {0.5, 0.5, 0.0 }), 0.4781203354, EPS);
    assertEquals(DIST.getCDF(new double[] {0.5, 0.5, -0.5 }), 0.4192231090, EPS);
    assertEquals(DIST.getCDF(new double[] {0.0, -1.0, -1.0 }), 0.00000000, EPS);
  }
}
