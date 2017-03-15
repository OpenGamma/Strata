/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.statistics.descriptive.MeanCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.MedianCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.PopulationVarianceCalculator;

/**
 * Test.
 */
@Test
public class GeneralizedParetoDistributionTest extends ProbabilityDistributionTestCase {
  private static final double MU = 0.4;
  private static final double SIGMA = 1.4;
  private static final double KSI = 0.2;
  private static final GeneralizedParetoDistribution DIST = new GeneralizedParetoDistribution(MU, SIGMA, KSI, ENGINE);
  private static final double LARGE_X = 1e20;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadSigma() {
    new GeneralizedParetoDistribution(MU, -SIGMA, KSI);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testZeroKsi() {
    new GeneralizedParetoDistribution(MU, SIGMA, 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEngine() {
    new GeneralizedParetoDistribution(MU, SIGMA, KSI, null);
  }

  @Test
  public void testBadInputs() {
    assertCDFWithNull(DIST);
    assertPDFWithNull(DIST);
  }

  @Test
  public void testObject() {
    assertEquals(KSI, DIST.getKsi(), 0);
    assertEquals(MU, DIST.getMu(), 0);
    assertEquals(SIGMA, DIST.getSigma(), 0);
    GeneralizedParetoDistribution other = new GeneralizedParetoDistribution(MU, SIGMA, KSI, ENGINE);
    assertEquals(DIST, other);
    assertEquals(DIST.hashCode(), other.hashCode());
    other = new GeneralizedParetoDistribution(MU, SIGMA, KSI);
    assertEquals(DIST, other);
    assertEquals(DIST.hashCode(), other.hashCode());
    other = new GeneralizedParetoDistribution(MU + 1, SIGMA, KSI);
    assertFalse(other.equals(DIST));
    other = new GeneralizedParetoDistribution(MU, SIGMA + 1, KSI);
    assertFalse(other.equals(DIST));
    other = new GeneralizedParetoDistribution(MU, SIGMA, KSI + 1);
    assertFalse(other.equals(DIST));
  }

  @Test
  public void testSupport() {
    ProbabilityDistribution<Double> dist = new GeneralizedParetoDistribution(MU, SIGMA, KSI, ENGINE);
    assertLimit(dist, MU - EPS);
    assertEquals(dist.getCDF(MU + EPS), 0, EPS);
    assertEquals(dist.getCDF(LARGE_X), 1, EPS);
    dist = new GeneralizedParetoDistribution(MU, SIGMA, -KSI);
    final double limit = MU + SIGMA / KSI;
    assertLimit(dist, MU - EPS);
    assertLimit(dist, limit + EPS);
    assertEquals(dist.getCDF(MU + EPS), 0, EPS);
    assertEquals(dist.getCDF(limit - 1e-15), 1, EPS);
  }

  @Test
  public void testDistribution() {
    final Function<double[], Double> meanCalculator = new MeanCalculator();
    final Function<double[], Double> medianCalculator = new MedianCalculator();
    final Function<double[], Double> varianceCalculator = new PopulationVarianceCalculator();
    final int n = 1000000;
    final double eps = 0.1;
    final double[] data = new double[n];
    for (int i = 0; i < n; i++) {
      data[i] = DIST.nextRandom();
    }
    final double mean = MU + SIGMA / (1 - KSI);
    final double median = MU + SIGMA * (Math.pow(2, KSI) - 1) / KSI;
    final double variance = SIGMA * SIGMA / ((1 - KSI) * (1 - KSI) * (1 - 2 * KSI));
    assertEquals(meanCalculator.apply(data), mean, eps);
    assertEquals(medianCalculator.apply(data), median, eps);
    assertEquals(varianceCalculator.apply(data), variance, eps);
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
