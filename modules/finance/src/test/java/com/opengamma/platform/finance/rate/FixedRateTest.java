/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class FixedRateTest {

  public void test_of() {
    FixedRate test = FixedRate.of(0.05);
    FixedRate expected = FixedRate.builder()
        .rate(0.05)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedRate test = FixedRate.of(0.05);
    coverImmutableBean(test);
  }

  public void test_serialization() {
    FixedRate test = FixedRate.of(0.05);
    assertSerialization(test);
  }

}
