/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Tests {@link SimpleOptionData}.
 */
@Test
public class SimpleOptionDataTest {

  private static final double FORWARD = 0.05;
  private static final double STRIKE = 0.1;
  private static final double TIME = 0.2;
  private static final double DF = 0.9;

  public void test_of() {
    SimpleOptionData test = SimpleOptionData.of(FORWARD, STRIKE, TIME, DF, CALL);
    assertEquals(test.getForward(), FORWARD);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getTimeToExpiry(), TIME);
    assertEquals(test.getDiscountFactor(), DF);
    assertEquals(test.getPutCall(), CALL);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleOptionData test = SimpleOptionData.of(FORWARD, STRIKE, TIME, DF, CALL);
    coverImmutableBean(test);
    SimpleOptionData test2 = SimpleOptionData.of(0.06, 0.96, 0.02, 0.95, PUT);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SimpleOptionData test = SimpleOptionData.of(FORWARD, STRIKE, TIME, DF, CALL);
    assertSerialization(test);
  }

}
