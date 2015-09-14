/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link BondGroup}.
 */
@Test
public class BondGroupTest {

  public void coverage() {
    BondGroup test = BondGroup.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
