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
 * Test {@link SimpleStrike}.
 */
@Test
public class SimpleStrikeTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    SimpleStrike test = SimpleStrike.of(0.6d);
    assertEquals(test.getType(), StrikeType.STRIKE);
    assertEquals(test.getValue(), 0.6d, 0d);
    assertEquals(test.getLabel(), "Strike=0.6");
    assertEquals(test.withValue(0.2d), SimpleStrike.of(0.2d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleStrike test = SimpleStrike.of(0.6d);
    coverImmutableBean(test);
    SimpleStrike test2 = SimpleStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SimpleStrike test = SimpleStrike.of(0.6d);
    assertSerialization(test);
  }

}
