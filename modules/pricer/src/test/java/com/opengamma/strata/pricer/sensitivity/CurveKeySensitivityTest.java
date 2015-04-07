/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link CurveSensitivityKey}.
 */
@Test
public class CurveKeySensitivityTest {

  private static final String NAME = "USD-LIBOR-3M";

  public void test_of() {
    CurveSensitivityKey test = CurveSensitivityKey.of(NAME);
    assertEquals(test.getCurveName(), NAME);
  }

  public void coverage() {
    CurveSensitivityKey test = CurveSensitivityKey.of(NAME);
    coverImmutableBean(test);
    CurveSensitivityKey test2 = CurveSensitivityKey.of("Other");
    coverBeanEquals(test, test2);
  }

}
