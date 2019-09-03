/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class StudentTDistributionTest extends ProbabilityDistributionTestCase {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double[] X =
      new double[] {0.32492, 0.270722, 0.717558, 1.372184, 1.36343, 1.770933, 2.13145, 2.55238, 2.80734, 3.6896};
  private static final double[] DOF = new double[] {1, 4, 6, 10, 11, 13, 15, 18, 23, 27};
  private static final double[] P = new double[] {0.6, 0.6, 0.75, 0.9, 0.9, 0.95, 0.975, 0.99, 0.995, 0.9995};

  @Test
  public void testNegativeDOF1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new StudentTDistribution(-2));
  }

  @Test
  public void testNegativeDOF2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new StudentTDistribution(-2, ENGINE));
  }

  @Test
  public void testNullEngine() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new StudentTDistribution(2, null));
  }

  @Test
  public void test() {
    ProbabilityDistribution<Double> dist = new StudentTDistribution(1, ENGINE);
    assertCDFWithNull(dist);
    assertPDFWithNull(dist);
    assertInverseCDF(X, dist);
    for (int i = 0; i < 10; i++) {
      dist = new StudentTDistribution(DOF[i], ENGINE);
      assertThat(P[i]).isCloseTo(dist.getCDF(X[i]), offset(EPS));
    }
  }

  @Test
  public void testNormal() {
    final ProbabilityDistribution<Double> highDOF = new StudentTDistribution(1000000, ENGINE);
    final ProbabilityDistribution<Double> normal = new NormalDistribution(0, 1, ENGINE);
    final double eps = 1e-4;
    double x;
    for (int i = 0; i < 100; i++) {
      x = RANDOM.nextDouble();
      assertThat(highDOF.getCDF(x)).isCloseTo(normal.getCDF(x), offset(eps));
      assertThat(highDOF.getPDF(x)).isCloseTo(normal.getPDF(x), offset(eps));
      assertThat(highDOF.getInverseCDF(x)).isCloseTo(normal.getInverseCDF(x), offset(eps));
    }
  }

  @Test
  public void testObject() {
    final double dof = 2.4;
    final StudentTDistribution dist = new StudentTDistribution(dof, ENGINE);
    StudentTDistribution other = new StudentTDistribution(dof, ENGINE);
    assertThat(dist).isEqualTo(other);
    assertThat(dist.hashCode()).isEqualTo(other.hashCode());
    other = new StudentTDistribution(dof);
    assertThat(dist).isEqualTo(other);
    assertThat(dist.hashCode()).isEqualTo(other.hashCode());
    other = new StudentTDistribution(dof + 1, ENGINE);
    assertThat(dist.equals(other)).isFalse();
  }
}
