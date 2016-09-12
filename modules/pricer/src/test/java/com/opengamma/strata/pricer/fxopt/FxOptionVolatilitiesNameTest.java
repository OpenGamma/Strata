/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link FxOptionVolatilitiesName}.
 */
@Test
public class FxOptionVolatilitiesNameTest {

  public void test_of() {
    FxOptionVolatilitiesName test = FxOptionVolatilitiesName.of("Foo");
    assertEquals(test.getName(), "Foo");
    assertEquals(test.getMarketDataType(), FxOptionVolatilities.class);
    assertEquals(test.toString(), "Foo");
    assertEquals(test.compareTo(FxOptionVolatilitiesName.of("Goo")) < 0, true);
  }

}
