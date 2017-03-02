/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;

import org.testng.annotations.Test;

/**
 * Test {@link NoRounding}.
 */
@Test
public class NoRoundingTest {

  public void test_none() {
    Rounding test = Rounding.none();
    assertEquals(test.toString(), "No rounding");
    assertEquals(Rounding.none(), test);
  }

  //-------------------------------------------------------------------------
  public void round_double_NONE() {
    assertEquals(Rounding.none().round(1.23d), 1.23d);
  }

  public void round_BigDecimal_NONE() {
    assertEquals(Rounding.none().round(BigDecimal.valueOf(1.23d)), BigDecimal.valueOf(1.23d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(NoRounding.INSTANCE);
  }

  public void test_serialization() {
    Rounding test = Rounding.none();
    assertSerialization(test);
  }

}
