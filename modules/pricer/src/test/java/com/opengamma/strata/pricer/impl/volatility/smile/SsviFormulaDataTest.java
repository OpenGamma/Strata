/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link SsviFormulaData}.
 */
@Test
public class SsviFormulaDataTest {

  private static final double SIGMA = 0.20;
  private static final double RHO = -0.20;
  private static final double ETA = 0.50;
  private static final SsviFormulaData DATA = SsviFormulaData.of(SIGMA, RHO, ETA);

  @Test
  public void test() {
    assertEquals(DATA.getSigma(), SIGMA, 0);
    assertEquals(DATA.getRho(), RHO, 0);
    assertEquals(DATA.getEta(), ETA, 0);
    assertEquals(DATA.getParameter(0), SIGMA, 0);
    assertEquals(DATA.getParameter(1), RHO, 0);
    assertEquals(DATA.getParameter(2), ETA, 0);
    assertEquals(DATA.getNumberOfParameters(), 3);
    SsviFormulaData other = SsviFormulaData.of(new double[] {SIGMA, RHO, ETA});
    assertEquals(other, DATA);
    assertEquals(other.hashCode(), DATA.hashCode());

    other = other.with(0, SIGMA - 0.01);
    assertFalse(other.equals(DATA));
    other = SsviFormulaData.of(SIGMA * 0.5, RHO, ETA);
    assertFalse(other.equals(DATA));
    other = SsviFormulaData.of(SIGMA, RHO * 0.5, ETA);
    assertFalse(other.equals(DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void testNegativeEta() {
    assertThrowsIllegalArg(() -> SsviFormulaData.of(SIGMA, RHO, -ETA));
  }

  @Test
  public void testNegativeSigma() {
    assertThrowsIllegalArg(() -> SsviFormulaData.of(-SIGMA, RHO, ETA));
  }

  @Test
  public void testLowRho() {
    assertThrowsIllegalArg(() -> SsviFormulaData.of(SIGMA, RHO - 10, ETA));
  }

  @Test
  public void testHighRho() {
    assertThrowsIllegalArg(() -> SsviFormulaData.of(SIGMA, RHO + 10, ETA));
  }

  @Test
  public void testWrongIndex() {
    assertThrowsIllegalArg(() -> DATA.isAllowed(-1, ETA));
  }

  @Test
  public void testWrongParameterLength() {
    assertThrowsIllegalArg(() -> SsviFormulaData.of(new double[] {ETA, RHO, SIGMA, 0.1}));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(DATA);
    SsviFormulaData another = SsviFormulaData.of(1.2, 0.4, 0.2);
    coverBeanEquals(DATA, another);
  }

  public void test_serialization() {
    assertSerialization(DATA);
  }

}
