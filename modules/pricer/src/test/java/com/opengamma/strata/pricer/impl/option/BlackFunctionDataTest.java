/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link BlackFunctionData}.
 */
@Test
public class BlackFunctionDataTest {

  private static final double FORWARD = 100;
  private static final double NUMERAIRE = 0.95;
  private static final double VOLATILITY = 0.23;

  public void test_of() {
    NormalFunctionData test = NormalFunctionData.of(FORWARD, NUMERAIRE, VOLATILITY);
    assertEquals(test.getForward(), FORWARD);
    assertEquals(test.getNumeraire(), NUMERAIRE);
    assertEquals(test.getNormalVolatility(), VOLATILITY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackFunctionData test = BlackFunctionData.of(FORWARD, NUMERAIRE, VOLATILITY);
    coverImmutableBean(test);
    BlackFunctionData test2 = BlackFunctionData.of(0.06, 0.96, 0.02);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    BlackFunctionData test = BlackFunctionData.of(FORWARD, NUMERAIRE, VOLATILITY);
    assertSerialization(test);
  }

}
