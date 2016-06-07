/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link Measures}.
 */
@Test
public class MeasuresTest {

  public void test_standard() {
    assertEquals(Measures.PRESENT_VALUE.isCurrencyConvertible(), true);
  }

  public void coverage() {
    coverPrivateConstructor(Measures.class);
  }

}
