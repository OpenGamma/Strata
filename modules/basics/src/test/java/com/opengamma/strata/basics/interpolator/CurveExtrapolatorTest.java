/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.interpolator;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;

import org.testng.annotations.Test;

@Test
public class CurveExtrapolatorTest {

  public void of() {
    String regex = ".*name not found.*";
    assertThrows(() -> CurveExtrapolator.of("UnknownInterpolator"), IllegalArgumentException.class, regex);
  }

  public void extendedEnum() {
    assertThat(CurveExtrapolator.extendedEnum()).isNotNull();
  }
}
