/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link SwaptionVolatilitiesName}.
 */
@Test
public class SwaptionVolatilitiesNameTest {

  public void test_of() {
    SwaptionVolatilitiesName test = SwaptionVolatilitiesName.of("Foo");
    assertEquals(test.getName(), "Foo");
    assertEquals(test.getMarketDataType(), SwaptionVolatilities.class);
    assertEquals(test.toString(), "Foo");
    assertEquals(test.compareTo(SwaptionVolatilitiesName.of("Goo")) < 0, true);
  }

}
