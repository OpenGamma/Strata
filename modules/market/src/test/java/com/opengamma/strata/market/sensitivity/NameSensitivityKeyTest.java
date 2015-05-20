/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link NameSensitivityKey}.
 */
@Test
public class NameSensitivityKeyTest {

  private static final String NAME = "USD-LIBOR-3M";

  public void test_of() {
    NameSensitivityKey test = NameSensitivityKey.of(NAME);
    assertEquals(test.getCurveName(), NAME);
  }

  public void coverage() {
    NameSensitivityKey test = NameSensitivityKey.of(NAME);
    coverImmutableBean(test);
    NameSensitivityKey test2 = NameSensitivityKey.of("Other");
    coverBeanEquals(test, test2);
  }

}
