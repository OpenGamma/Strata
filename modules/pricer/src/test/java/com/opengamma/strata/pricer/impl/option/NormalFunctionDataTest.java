/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Tests {@link NormalFunctionData}.
 */
@Test
public class NormalFunctionDataTest {

  private static final double FORWARD = 0.05;
  private static final double NUMERAIRE = 0.95;
  private static final double VOLATILITY = 0.01;
  private static final NormalFunctionData NORMAL_DATA = new NormalFunctionData(FORWARD, NUMERAIRE, VOLATILITY);

  public void getter() {
    assertEquals(NORMAL_DATA.getForward(), FORWARD);
    assertEquals(NORMAL_DATA.getNumeraire(), NUMERAIRE);
    assertEquals(NORMAL_DATA.getNormalVolatility(), VOLATILITY);
  }

}
