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
 * Test {@link MoneynessStrike}.
 */
@Test
public class MoneynessStrikeTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    MoneynessStrike test = MoneynessStrike.of(0.6d);
    assertEquals(test.getType(), StrikeType.MONEYNESS);
    assertEquals(test.getValue(), 0.6d, 0d);
    assertEquals(test.getLabel(), "Moneyness=0.6");
    assertEquals(test.withValue(0.2d), MoneynessStrike.of(0.2d));
  }

  public void test_ofStrikeAndForward() {
    MoneynessStrike test = MoneynessStrike.ofStrikeAndForward(0.6d, 1.2d);
    assertEquals(test.getType(), StrikeType.MONEYNESS);
    assertEquals(test.getValue(), 0.5d, 0d);
    assertEquals(test.getLabel(), "Moneyness=0.5");
    assertEquals(test.withValue(0.2d), MoneynessStrike.of(0.2d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    MoneynessStrike test = MoneynessStrike.of(0.6d);
    coverImmutableBean(test);
    MoneynessStrike test2 = MoneynessStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    MoneynessStrike test = MoneynessStrike.of(0.6d);
    assertSerialization(test);
  }

}
