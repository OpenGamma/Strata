/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link Surface}.
 */
@Test
public class SurfaceTest {

  private static final Surface SURFACE = new TestingSurface();

  public void test_applyPerturbation() {
    Surface result = ConstantNodalSurface.of("Test", 2d);
    assertThat(SURFACE.applyPerturbation(surface -> result)).isSameAs(result);
  }

  public void test_toNodalSurface() {
    assertThrows(() -> SURFACE.toNodalSurface(), UnsupportedOperationException.class);
  }

}
