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
public class ChiSquareDistributionTest extends ProbabilityDistributionTestCase {
  private static final double[] X = new double[] {1.9, 5.8, 9.0, 15.5, 39};
  private static final double[] DOF = new double[] {3, 6, 7, 16, 28};
  private static final double[] Q = new double[] {0.59342, 0.44596, 0.25266, 0.48837, 0.08092};
  private static final ChiSquareDistribution DIST = new ChiSquareDistribution(1, ENGINE);

  @Test
  public void testNegativeDOF1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ChiSquareDistribution(-2));
  }

  @Test
  public void testNegativeDOF2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ChiSquareDistribution(-2, ENGINE));
  }

  @Test
  public void testNullEngine() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ChiSquareDistribution(2, null));
  }

  @Test
  public void testObject() {
    assertThat(DIST.getDegreesOfFreedom()).isEqualTo(1);
    ChiSquareDistribution other = new ChiSquareDistribution(1);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new ChiSquareDistribution(1, ENGINE);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new ChiSquareDistribution(2);
    assertThat(other).isNotEqualTo(DIST);
  }

  @Test
  public void test() {
    assertCDFWithNull(DIST);
    assertPDFWithNull(DIST);
    assertInverseCDFWithNull(DIST);
    ChiSquareDistribution dist;
    for (int i = 0; i < 5; i++) {
      dist = new ChiSquareDistribution(DOF[i], ENGINE);
      assertThat(1 - dist.getCDF(X[i])).isCloseTo(Q[i], offset(EPS));
      assertThat(dist.getInverseCDF(dist.getCDF(X[i]))).isCloseTo(X[i], offset(EPS));
    }
  }
}
