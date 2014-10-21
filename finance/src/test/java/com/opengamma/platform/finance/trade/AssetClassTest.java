/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.trade;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link AssetClass}.
 */
@Test
public class AssetClassTest {

  //-----------------------------------------------------------------------
  public void test_constants() {
    assertEquals(AssetClass.EQUITY.name(), "EQUITY");
  }

}
