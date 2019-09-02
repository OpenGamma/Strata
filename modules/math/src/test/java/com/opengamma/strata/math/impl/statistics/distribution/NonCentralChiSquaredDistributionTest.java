/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class NonCentralChiSquaredDistributionTest {
  private static final double DOF = 3;
  private static final double NON_CENTRALITY = 1.5;
  private static final NonCentralChiSquaredDistribution DIST = new NonCentralChiSquaredDistribution(DOF, NON_CENTRALITY);

  @Test
  public void testNegativeDOF() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new NonCentralChiSquaredDistribution(-DOF, NON_CENTRALITY));
  }

  @Test
  public void testNegativeNonCentrality() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new NonCentralChiSquaredDistribution(DOF, -NON_CENTRALITY));
  }

  @Test
  public void testNullX() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DIST.getCDF(null));
  }

  @Test
  public void testInverseCDF() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> DIST.getInverseCDF(0.5));
  }

  @Test
  public void testPDF() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> DIST.getPDF(0.5));
  }

  @Test
  public void testRandom() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> DIST.nextRandom());
  }

  @Test
  public void test() {
    assertThat(DIST.getDegrees()).isEqualTo(DOF);
    assertThat(DIST.getNonCentrality()).isEqualTo(NON_CENTRALITY);
    assertThat(DIST.getCDF(-100.)).isEqualTo(0);
    assertThat(DIST.getCDF(0.)).isEqualTo(0);
    assertThat(DIST.getCDF(5.)).isCloseTo(0.649285, offset(1e-6));
  }

  @Test
  public void testObject() {
    assertThat(DIST.getDegrees()).isEqualTo(DOF);
    assertThat(DIST.getNonCentrality()).isEqualTo(NON_CENTRALITY);
    NonCentralChiSquaredDistribution other = new NonCentralChiSquaredDistribution(DOF, NON_CENTRALITY);
    assertThat(DIST).isEqualTo(other);
    assertThat(DIST.hashCode()).isEqualTo(other.hashCode());
    other = new NonCentralChiSquaredDistribution(DOF + 1, NON_CENTRALITY);
    assertThat(other.equals(DIST)).isFalse();
    other = new NonCentralChiSquaredDistribution(DOF, NON_CENTRALITY + 1);
    assertThat(other.equals(DIST)).isFalse();
  }

  /**
   * Numbers computed from R
   */
  @Test
  public void testLargeValues() {
    double x = 123;
    double dof = 6.4;
    double nonCent = 100.34;
    NonCentralChiSquaredDistribution dist = new NonCentralChiSquaredDistribution(dof, nonCent);
    assertThat(0.7930769).isCloseTo(dist.getCDF(x), offset(1e-6));

    x = 455.038;
    dof = 12;
    nonCent = 444.44;

    dist = new NonCentralChiSquaredDistribution(dof, nonCent);
    assertThat(0.4961805).isCloseTo(dist.getCDF(x), offset(1e-6));

    x = 999400;
    dof = 500;
    nonCent = 1000000;
    dist = new NonCentralChiSquaredDistribution(dof, nonCent);
    assertThat(0.2913029).isCloseTo(dist.getCDF(x), offset(1e-6));

  }

  /**
   * Numbers computed from R
   */
  @Test
  public void debugTest() {
    final double dof = 3.666;
    final double nonCentrality = 75;
    final double x = 13.89;

    final NonCentralChiSquaredDistribution chiSq1 = new NonCentralChiSquaredDistribution(dof, nonCentrality);
    final double y1 = Math.log(chiSq1.getCDF(x));
    assertThat(-15.92129).isCloseTo(y1, offset(1e-5));
  }

}
