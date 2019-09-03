/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SsviFormulaData}.
 */
public class SsviFormulaDataTest {

  private static final double SIGMA = 0.20;
  private static final double RHO = -0.20;
  private static final double ETA = 0.50;
  private static final SsviFormulaData DATA = SsviFormulaData.of(SIGMA, RHO, ETA);

  @Test
  public void test() {
    assertThat(DATA.getSigma()).isEqualTo(SIGMA);
    assertThat(DATA.getRho()).isEqualTo(RHO);
    assertThat(DATA.getEta()).isEqualTo(ETA);
    assertThat(DATA.getParameter(0)).isEqualTo(SIGMA);
    assertThat(DATA.getParameter(1)).isEqualTo(RHO);
    assertThat(DATA.getParameter(2)).isEqualTo(ETA);
    assertThat(DATA.getNumberOfParameters()).isEqualTo(3);
    SsviFormulaData other = SsviFormulaData.of(new double[] {SIGMA, RHO, ETA});
    assertThat(other).isEqualTo(DATA);
    assertThat(other.hashCode()).isEqualTo(DATA.hashCode());

    other = other.with(0, SIGMA - 0.01);
    assertThat(other.equals(DATA)).isFalse();
    other = SsviFormulaData.of(SIGMA * 0.5, RHO, ETA);
    assertThat(other.equals(DATA)).isFalse();
    other = SsviFormulaData.of(SIGMA, RHO * 0.5, ETA);
    assertThat(other.equals(DATA)).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void testNegativeEta() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SsviFormulaData.of(SIGMA, RHO, -ETA));
  }

  @Test
  public void testNegativeSigma() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SsviFormulaData.of(-SIGMA, RHO, ETA));
  }

  @Test
  public void testLowRho() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SsviFormulaData.of(SIGMA, RHO - 10, ETA));
  }

  @Test
  public void testHighRho() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SsviFormulaData.of(SIGMA, RHO + 10, ETA));
  }

  @Test
  public void testWrongIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DATA.isAllowed(-1, ETA));
  }

  @Test
  public void testWrongParameterLength() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SsviFormulaData.of(new double[] {ETA, RHO, SIGMA, 0.1}));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(DATA);
    SsviFormulaData another = SsviFormulaData.of(1.2, 0.4, 0.2);
    coverBeanEquals(DATA, another);
  }

  @Test
  public void test_serialization() {
    assertSerialization(DATA);
  }

}
