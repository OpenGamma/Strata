/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link SurfaceName}.
 */
@Test
public class SurfaceNameTest {

  public void coverage() {
    SurfaceName test = SurfaceName.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
