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

import com.opengamma.strata.math.impl.statistics.descriptive.MeanCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.MedianCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleFisherKurtosisCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleSkewnessCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleVarianceCalculator;

/**
 * Test.
 */
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

  @Test
  public void testNegativeBDistribution() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LaplaceDistribution(1, -0.4));
  }

  @Test
  public void testNullEngine() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LaplaceDistribution(0, 1, null));
  }

  @Test
  public void testInverseCDFWithLow() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LAPLACE.getInverseCDF(-0.45));
  }

  @Test
  public void testInverseCDFWithHigh() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LAPLACE.getInverseCDF(6.7));
  }

  @Test
  public void testObject() {
    assertThat(LAPLACE.getB()).isEqualTo(B);
    assertThat(LAPLACE.getMu()).isEqualTo(MU);
    LaplaceDistribution other = new LaplaceDistribution(MU, B);
    assertThat(LAPLACE).isEqualTo(other);
    assertThat(LAPLACE.hashCode()).isEqualTo(other.hashCode());
    other = new LaplaceDistribution(MU + 1, B);
    assertThat(LAPLACE.equals(other)).isFalse();
    other = new LaplaceDistribution(MU, B + 1);
    assertThat(LAPLACE.equals(other)).isFalse();
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
    assertThat(mean).isCloseTo(MU, offset(EPS1));
    assertThat(median).isCloseTo(MU, offset(EPS1));
    assertThat(variance).isCloseTo(2 * B * B, offset(EPS1));
    assertThat(skew).isCloseTo(0, offset(EPS1));
    assertThat(kurtosis).isCloseTo(3, offset(EPS1));
  }
}
