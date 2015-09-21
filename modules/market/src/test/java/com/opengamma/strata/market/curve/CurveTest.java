/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link Curve}.
 */
@Test
public class CurveTest {

  private static final Curve CURVE = new TestingCurve();

  public void test_applyPerturbation() {
    Curve result = ConstantNodalCurve.of("Test", 2d);
    assertThat(CURVE.applyPerturbation(curve -> result)).isSameAs(result);
  }

  public void test_toNodalCurve() {
    assertThrows(() -> CURVE.toNodalCurve(), UnsupportedOperationException.class);
  }

}
