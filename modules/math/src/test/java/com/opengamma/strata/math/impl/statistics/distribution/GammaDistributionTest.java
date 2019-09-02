/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class GammaDistributionTest extends ProbabilityDistributionTestCase {
  private static final double K = 1;
  private static final double THETA = 0.5;
  private static final GammaDistribution DIST = new GammaDistribution(K, THETA, ENGINE);

  @Test
  public void testNegativeK1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GammaDistribution(-1, 1));
  }

  @Test
  public void testNegativeK2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GammaDistribution(-1, 1, ENGINE));
  }

  @Test
  public void testNegativeTheta1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GammaDistribution(1, -1));
  }

  @Test
  public void testNegativeTheta2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GammaDistribution(1, -1, ENGINE));
  }

  @Test
  public void testNullEngine() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GammaDistribution(1, 1, null));
  }

  @Test
  public void test() {
    assertCDFWithNull(DIST);
    assertPDFWithNull(DIST);
    assertThat(K).isEqualTo(DIST.getK());
    assertThat(THETA).isEqualTo(DIST.getTheta());
    GammaDistribution other = new GammaDistribution(K, THETA, ENGINE);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new GammaDistribution(K, THETA);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new GammaDistribution(K + 1, THETA);
    assertThat(other.equals(DIST)).isFalse();
    other = new GammaDistribution(K, THETA + 1);
    assertThat(other.equals(DIST)).isFalse();
  }
}
