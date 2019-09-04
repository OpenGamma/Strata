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
public class BivariateNormalDistributionTest {
  private static final ProbabilityDistribution<double[]> DIST = new BivariateNormalDistribution();
  private static final double EPS = 1e-8;

  @Test
  public void testNullCDF() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DIST.getCDF(null));
  }

  @Test
  public void testInsufficientLengthCDF() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DIST.getCDF(new double[] {2, 1}));
  }

  @Test
  public void testExcessiveLengthCDF() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DIST.getCDF(new double[] {2, 1, 4, 5}));
  }

  @Test
  public void testHighCorrelation() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DIST.getCDF(new double[] {1., 1., 3.}));
  }

  @Test
  public void testLowCorrelation() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DIST.getCDF(new double[] {1., 1., -3.}));
  }

  @Test
  public void test() {
    assertThat(DIST.getCDF(new double[] {Double.POSITIVE_INFINITY, Math.random(), Math.random()})).isEqualTo(1);
    assertThat(DIST.getCDF(new double[] {Math.random(), Double.POSITIVE_INFINITY, Math.random()})).isEqualTo(1);
    assertThat(DIST.getCDF(new double[] {Double.NEGATIVE_INFINITY, Math.random(), Math.random()})).isEqualTo(0);
    assertThat(DIST.getCDF(new double[] {Math.random(), Double.NEGATIVE_INFINITY, Math.random()})).isEqualTo(0);
    assertThat(DIST.getCDF(new double[] {0.0, 0.0, 0.0})).isCloseTo(0.25, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.0, 0.0, -0.5})).isCloseTo(1. / 6, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.0, 0.0, 0.5})).isCloseTo(1. / 3, offset(EPS));

    assertThat(DIST.getCDF(new double[] {0.0, -0.5, 0.0})).isCloseTo(0.1542687694, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.0, -0.5, -0.5})).isCloseTo(0.0816597607, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.0, -0.5, 0.5})).isCloseTo(0.2268777781, offset(EPS));

    assertThat(DIST.getCDF(new double[] {0.0, 0.5, 0.0})).isCloseTo(0.3457312306, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.0, 0.5, -0.5})).isCloseTo(0.2731222219, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.0, 0.5, 0.5})).isCloseTo(0.4183402393, offset(EPS));

    assertThat(DIST.getCDF(new double[] {-0.5, 0.0, 0.0})).isCloseTo(0.1542687694, offset(EPS));
    assertThat(DIST.getCDF(new double[] {-0.5, 0.0, -0.5})).isCloseTo(0.0816597607, offset(EPS));
    assertThat(DIST.getCDF(new double[] {-0.5, 0.0, 0.5})).isCloseTo(0.2268777781, offset(EPS));

    assertThat(DIST.getCDF(new double[] {-0.5, -0.5, 0.0})).isCloseTo(0.0951954128, offset(EPS));
    assertThat(DIST.getCDF(new double[] {-0.5, -0.5, -0.5})).isCloseTo(0.0362981865, offset(EPS));
    assertThat(DIST.getCDF(new double[] {-0.5, -0.5, 0.5})).isCloseTo(0.1633195213, offset(EPS));

    assertThat(DIST.getCDF(new double[] {-0.5, 0.5, 0.0})).isCloseTo(0.2133421259, offset(EPS));
    assertThat(DIST.getCDF(new double[] {-0.5, 0.5, -0.5})).isCloseTo(0.1452180174, offset(EPS));
    assertThat(DIST.getCDF(new double[] {-0.5, 0.5, 0.5})).isCloseTo(0.2722393522, offset(EPS));

    assertThat(DIST.getCDF(new double[] {0.5, 0.0, 0.0})).isCloseTo(0.3457312306, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.5, 0.0, -0.5})).isCloseTo(0.2731222219, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.5, 0.0, 0.5})).isCloseTo(0.4183402393, offset(EPS));

    assertThat(DIST.getCDF(new double[] {0.5, -0.5, 0.0})).isCloseTo(0.2133421259, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.5, -0.5, -0.5})).isCloseTo(0.1452180174, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.5, -0.5, 0.5})).isCloseTo(0.2722393522, offset(EPS));

    assertThat(DIST.getCDF(new double[] {0.5, 0.5, 0.0})).isCloseTo(0.4781203354, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.5, 0.5, -0.5})).isCloseTo(0.4192231090, offset(EPS));
    assertThat(DIST.getCDF(new double[] {0.0, -1.0, -1.0})).isCloseTo(0.00000000, offset(EPS));
  }
}
