/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.statistics.descriptive.MeanCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.MedianCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.PopulationVarianceCalculator;

/**
 * Test.
 */
public class GeneralizedParetoDistributionTest extends ProbabilityDistributionTestCase {
  private static final double MU = 0.4;
  private static final double SIGMA = 1.4;
  private static final double KSI = 0.2;
  private static final GeneralizedParetoDistribution DIST = new GeneralizedParetoDistribution(MU, SIGMA, KSI, ENGINE);
  private static final double LARGE_X = 1e20;

  @Test
  public void testBadSigma() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneralizedParetoDistribution(MU, -SIGMA, KSI));
  }

  @Test
  public void testZeroKsi() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneralizedParetoDistribution(MU, SIGMA, 0));
  }

  @Test
  public void testNullEngine() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneralizedParetoDistribution(MU, SIGMA, KSI, null));
  }

  @Test
  public void testBadInputs() {
    assertCDFWithNull(DIST);
    assertPDFWithNull(DIST);
  }

  @Test
  public void testObject() {
    assertThat(KSI).isEqualTo(DIST.getKsi());
    assertThat(MU).isEqualTo(DIST.getMu());
    assertThat(SIGMA).isEqualTo(DIST.getSigma());
    GeneralizedParetoDistribution other = new GeneralizedParetoDistribution(MU, SIGMA, KSI, ENGINE);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new GeneralizedParetoDistribution(MU, SIGMA, KSI);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new GeneralizedParetoDistribution(MU + 1, SIGMA, KSI);
    assertThat(other.equals(DIST)).isFalse();
    other = new GeneralizedParetoDistribution(MU, SIGMA + 1, KSI);
    assertThat(other.equals(DIST)).isFalse();
    other = new GeneralizedParetoDistribution(MU, SIGMA, KSI + 1);
    assertThat(other.equals(DIST)).isFalse();
  }

  @Test
  public void testSupport() {
    ProbabilityDistribution<Double> dist = new GeneralizedParetoDistribution(MU, SIGMA, KSI, ENGINE);
    assertLimit(dist, MU - EPS);
    assertThat(dist.getCDF(MU + EPS)).isCloseTo(0, offset(EPS));
    assertThat(dist.getCDF(LARGE_X)).isCloseTo(1, offset(EPS));
    dist = new GeneralizedParetoDistribution(MU, SIGMA, -KSI);
    final double limit = MU + SIGMA / KSI;
    assertLimit(dist, MU - EPS);
    assertLimit(dist, limit + EPS);
    assertThat(dist.getCDF(MU + EPS)).isCloseTo(0, offset(EPS));
    assertThat(dist.getCDF(limit - 1e-15)).isCloseTo(1, offset(EPS));
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
    assertThat(meanCalculator.apply(data)).isCloseTo(mean, offset(eps));
    assertThat(medianCalculator.apply(data)).isCloseTo(median, offset(eps));
    assertThat(varianceCalculator.apply(data)).isCloseTo(variance, offset(eps));
  }

  private void assertLimit(final ProbabilityDistribution<Double> dist, final double limit) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getCDF(limit));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getPDF(limit));
  }
}
