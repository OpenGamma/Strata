/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link LogMoneynessStrike}.
 */
@Test
public class LogMoneynessStrikeTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    LogMoneynessStrike test = LogMoneynessStrike.of(0.6d);
    assertEquals(test.getType(), StrikeType.LOG_MONEYNESS);
    assertEquals(test.getValue(), 0.6d, 0d);
    assertEquals(test.getLabel(), "LogMoneyness=0.6");
    assertEquals(test.withValue(0.2d), LogMoneynessStrike.of(0.2d));
  }

  public void test_ofStrikeAndForward() {
    LogMoneynessStrike test = LogMoneynessStrike.ofStrikeAndForward(0.6d, 1.2d);
    assertEquals(test.getType(), StrikeType.LOG_MONEYNESS);
    assertEquals(test.getValue(), Math.log(0.5d), 0d);
    assertEquals(test.getLabel(), "LogMoneyness=" + Math.log(0.5d));
    assertEquals(test.withValue(0.2d), LogMoneynessStrike.of(0.2d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LogMoneynessStrike test = LogMoneynessStrike.of(0.6d);
    coverImmutableBean(test);
    LogMoneynessStrike test2 = LogMoneynessStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    LogMoneynessStrike test = LogMoneynessStrike.of(0.6d);
    assertSerialization(test);
  }

}
