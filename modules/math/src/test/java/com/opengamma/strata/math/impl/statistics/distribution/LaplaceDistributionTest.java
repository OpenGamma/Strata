/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.statistics.descriptive.MeanCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.MedianCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleFisherKurtosisCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleSkewnessCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleVarianceCalculator;

/**
 * Test.
 */
@Test
public class LaplaceDistributionTest extends ProbabilityDistributionTestCase {
  private static final double MU = 0.7;
  private static final double B = 0.5;
  private static final LaplaceDistribution LAPLACE = new LaplaceDistribution(MU, B, ENGINE);
  private static final double[] DATA;
  private static final double EPS1 = 0.05;
  static {
    final int n = 1000000;
    DATA = new double[n];
    for (int i = 0; i < n; i++) {
      DATA[i] = LAPLACE.nextRandom();
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeBDistribution() {
    new LaplaceDistribution(1, -0.4);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEngine() {
    new LaplaceDistribution(0, 1, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInverseCDFWithLow() {
    LAPLACE.getInverseCDF(-0.45);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInverseCDFWithHigh() {
    LAPLACE.getInverseCDF(6.7);
  }

  @Test
  public void testObject() {
    assertEquals(LAPLACE.getB(), B, 0);
    assertEquals(LAPLACE.getMu(), MU, 0);
    LaplaceDistribution other = new LaplaceDistribution(MU, B);
    assertEquals(LAPLACE, other);
    assertEquals(LAPLACE.hashCode(), other.hashCode());
    other = new LaplaceDistribution(MU + 1, B);
    assertFalse(LAPLACE.equals(other));
    other = new LaplaceDistribution(MU, B + 1);
    assertFalse(LAPLACE.equals(other));
  }

  @Test
  public void test() {
    assertCDFWithNull(LAPLACE);
    assertPDFWithNull(LAPLACE);
    assertInverseCDFWithNull(LAPLACE);
    final double mean = new MeanCalculator().apply(DATA);
    final double median = new MedianCalculator().apply(DATA);
    final double variance = new SampleVarianceCalculator().apply(DATA);
    final double skew = new SampleSkewnessCalculator().apply(DATA);
    final double kurtosis = new SampleFisherKurtosisCalculator().apply(DATA);
    assertEquals(mean, MU, EPS1);
    assertEquals(median, MU, EPS1);
    assertEquals(variance, 2 * B * B, EPS1);
    assertEquals(skew, 0, EPS1);
    assertEquals(kurtosis, 3, EPS1);
  }
}
