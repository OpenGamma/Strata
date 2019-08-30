/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link SabrFormulaData}.
 */
public class SabrFormulaDataTest {

  private static final double NU = 0.8;
  private static final double RHO = -0.65;
  private static final double BETA = 0.76;
  private static final double ALPHA = 1.4;
  private static final SabrFormulaData DATA = SabrFormulaData.of(ALPHA, BETA, RHO, NU);

  @Test
  public void test() {
    assertThat(DATA.getAlpha()).isEqualTo(ALPHA);
    assertThat(DATA.getBeta()).isEqualTo(BETA);
    assertThat(DATA.getNu()).isEqualTo(NU);
    assertThat(DATA.getRho()).isEqualTo(RHO);
    assertThat(DATA.getParameter(0)).isEqualTo(ALPHA);
    assertThat(DATA.getParameter(1)).isEqualTo(BETA);
    assertThat(DATA.getParameter(2)).isEqualTo(RHO);
    assertThat(DATA.getParameter(3)).isEqualTo(NU);
    assertThat(DATA.getNumberOfParameters()).isEqualTo(4);
    SabrFormulaData other = SabrFormulaData.of(new double[] {ALPHA, BETA, RHO, NU});
    assertThat(other).isEqualTo(DATA);
    assertThat(other.hashCode()).isEqualTo(DATA.hashCode());

    other = other.with(0, ALPHA - 0.01);
    assertThat(other.equals(DATA)).isFalse();
    other = SabrFormulaData.of(ALPHA, BETA * 0.5, RHO, NU);
    assertThat(other.equals(DATA)).isFalse();
    other = SabrFormulaData.of(ALPHA, BETA, RHO, NU * 0.5);
    assertThat(other.equals(DATA)).isFalse();
    other = SabrFormulaData.of(ALPHA, BETA, RHO * 0.5, NU);
    assertThat(other.equals(DATA)).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void testNegativeBETA() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SabrFormulaData.of(ALPHA, -BETA, RHO, NU));
  }

  @Test
  public void testNegativeNu() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SabrFormulaData.of(ALPHA, BETA, RHO, -NU));
  }

  @Test
  public void testLowRho() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SabrFormulaData.of(ALPHA, BETA, RHO - 10, NU));
  }

  @Test
  public void testHighRho() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SabrFormulaData.of(ALPHA, BETA, RHO + 10, NU));
  }

  @Test
  public void testWrongIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DATA.isAllowed(-1, ALPHA));
  }

  @Test
  public void testWrongParameterLength() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SabrFormulaData.of(new double[] {ALPHA, BETA, RHO, NU, 0.1}));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(DATA);
    SabrFormulaData another = SabrFormulaData.of(1.2, 0.4, 0.0, 0.2);
    coverBeanEquals(DATA, another);
  }

  @Test
  public void test_serialization() {
    assertSerialization(DATA);
  }

}
