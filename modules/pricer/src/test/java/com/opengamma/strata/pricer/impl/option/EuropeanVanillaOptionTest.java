/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Test {@link EuropeanVanillaOption}.
 */
@Test
public class EuropeanVanillaOptionTest {

  private static final double STRIKE = 100;
  private static final double TIME = 0.5;

  public void testNegativeTime() {
    assertThrowsIllegalArg(() -> EuropeanVanillaOption.of(STRIKE, -TIME, CALL));
  }

  public void test_of() {
    EuropeanVanillaOption test = EuropeanVanillaOption.of(STRIKE, TIME, CALL);
    assertEquals(test.getStrike(), STRIKE, 0d);
    assertEquals(test.getTimeToExpiry(), TIME, 0d);
    assertEquals(test.getPutCall(), CALL);
    assertTrue(test.isCall());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    EuropeanVanillaOption test = EuropeanVanillaOption.of(STRIKE, TIME, CALL);
    coverImmutableBean(test);
    EuropeanVanillaOption test2 = EuropeanVanillaOption.of(110, 0.6, PUT);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    EuropeanVanillaOption test = EuropeanVanillaOption.of(STRIKE, TIME, CALL);
    assertSerialization(test);
  }

}
