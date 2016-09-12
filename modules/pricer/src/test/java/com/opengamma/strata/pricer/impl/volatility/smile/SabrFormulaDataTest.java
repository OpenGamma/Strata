/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.Test;

/**
 * Test {@link SabrFormulaData}.
 */
@Test
public class SabrFormulaDataTest {

  private static final double NU = 0.8;
  private static final double RHO = -0.65;
  private static final double BETA = 0.76;
  private static final double ALPHA = 1.4;
  private static final SabrFormulaData DATA = SabrFormulaData.of(ALPHA, BETA, RHO, NU);

  @Test
  public void test() {
    assertEquals(DATA.getAlpha(), ALPHA, 0);
    assertEquals(DATA.getBeta(), BETA, 0);
    assertEquals(DATA.getNu(), NU, 0);
    assertEquals(DATA.getRho(), RHO, 0);
    assertEquals(DATA.getParameter(0), ALPHA, 0);
    assertEquals(DATA.getParameter(1), BETA, 0);
    assertEquals(DATA.getParameter(2), RHO, 0);
    assertEquals(DATA.getParameter(3), NU, 0);
    assertEquals(DATA.getNumberOfParameters(), 4);
    SabrFormulaData other = SabrFormulaData.of(new double[] {ALPHA, BETA, RHO, NU});
    assertEquals(other, DATA);
    assertEquals(other.hashCode(), DATA.hashCode());

    other = other.with(0, ALPHA - 0.01);
    assertFalse(other.equals(DATA));
    other = SabrFormulaData.of(ALPHA, BETA * 0.5, RHO, NU);
    assertFalse(other.equals(DATA));
    other = SabrFormulaData.of(ALPHA, BETA, RHO, NU * 0.5);
    assertFalse(other.equals(DATA));
    other = SabrFormulaData.of(ALPHA, BETA, RHO * 0.5, NU);
    assertFalse(other.equals(DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testNegativeBETA() {
    assertThrowsIllegalArg(() -> SabrFormulaData.of(ALPHA, -BETA, RHO, NU));
  }

  @Test
  public void testNegativeNu() {
    assertThrowsIllegalArg(() -> SabrFormulaData.of(ALPHA, BETA, RHO, -NU));
  }

  @Test
  public void testLowRho() {
    assertThrowsIllegalArg(() -> SabrFormulaData.of(ALPHA, BETA, RHO - 10, NU));
  }

  @Test
  public void testHighRho() {
    assertThrowsIllegalArg(() -> SabrFormulaData.of(ALPHA, BETA, RHO + 10, NU));
  }

  @Test
  public void testWrongIndex() {
    assertThrowsIllegalArg(() -> DATA.isAllowed(-1, ALPHA));
  }

  @Test
  public void testWrongParameterLength() {
    assertThrowsIllegalArg(() -> SabrFormulaData.of(new double[] {ALPHA, BETA, RHO, NU, 0.1}));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(DATA);
    SabrFormulaData another = SabrFormulaData.of(1.2, 0.4, 0.0, 0.2);
    coverBeanEquals(DATA, another);
  }

  public void test_serialization() {
    assertSerialization(DATA);
  }

}
