/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertThrows;

import org.testng.annotations.Test;

/**
 * Test {@link Surface}.
 */
@Test
public class SurfaceTest {

  private static final Surface SURFACE = new TestingSurface();

  public void test_toNodalSurface() {
    assertThrows(() -> SURFACE.toNodalSurface(), UnsupportedOperationException.class);
  }

}
