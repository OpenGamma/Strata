/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class GeneralizedExtremeValueDistributionTest extends ProbabilityDistributionTestCase {
  private static final double MU = 1.5;
  private static final double SIGMA = 0.6;
  private static final double KSI = 0.7;
  private static final GeneralizedExtremeValueDistribution DIST = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI);
  private static final double LARGE_X = 1e10;

  @Test
  public void testBadConstructor() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GeneralizedExtremeValueDistribution(MU, -SIGMA, KSI));
  }

  @Test
  public void testBadInputs() {
    assertCDFWithNull(DIST);
    assertPDFWithNull(DIST);
  }

  @Test
  public void testObject() {
    assertThat(MU).isEqualTo(DIST.getMu());
    assertThat(SIGMA).isEqualTo(DIST.getSigma());
    assertThat(KSI).isEqualTo(DIST.getKsi());
    GeneralizedExtremeValueDistribution other = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new GeneralizedExtremeValueDistribution(MU + 1, SIGMA, KSI);
    assertThat(other.equals(DIST)).isFalse();
    other = new GeneralizedExtremeValueDistribution(MU, SIGMA + 1, KSI);
    assertThat(other.equals(DIST)).isFalse();
    other = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI + 1);
    assertThat(other.equals(DIST)).isFalse();
  }

  @Test
  public void testSupport() {
    ProbabilityDistribution<Double> dist = new GeneralizedExtremeValueDistribution(MU, SIGMA, KSI);
    double limit = MU - SIGMA / KSI;
    assertLimit(dist, limit - EPS);
    assertThat(dist.getCDF(limit + EPS)).isCloseTo(0, offset(EPS));
    assertThat(dist.getCDF(LARGE_X)).isCloseTo(1, offset(EPS));
    dist = new GeneralizedExtremeValueDistribution(MU, SIGMA, -KSI);
    limit = MU + SIGMA / KSI;
    assertLimit(dist, limit + EPS);
    assertThat(dist.getCDF(-LARGE_X)).isCloseTo(0, offset(EPS));
    assertThat(dist.getCDF(limit - EPS)).isCloseTo(1, offset(EPS));
    dist = new GeneralizedExtremeValueDistribution(MU, SIGMA, 0);
    assertThat(dist.getCDF(-LARGE_X)).isCloseTo(0, offset(EPS));
    assertThat(dist.getCDF(LARGE_X)).isCloseTo(1, offset(EPS));
  }

  private void assertLimit(final ProbabilityDistribution<Double> dist, final double limit) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getCDF(limit));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getPDF(limit));
  }
}
