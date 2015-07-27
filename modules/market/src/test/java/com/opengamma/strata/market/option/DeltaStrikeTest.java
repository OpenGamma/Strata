/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link DeltaStrike}.
 */
@Test
public class DeltaStrikeTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    DeltaStrike test = DeltaStrike.of(0.6d);
    assertEquals(test.getType(), StrikeType.DELTA);
    assertEquals(test.getValue(), 0.6d, 0d);
    assertEquals(test.getLabel(), "Delta=0.6");
    assertEquals(test.withValue(0.2d), DeltaStrike.of(0.2d));
  }

  public void test_of_invalid() {
    assertThrowsIllegalArg(() -> DeltaStrike.of(-0.001d));
    assertThrowsIllegalArg(() -> DeltaStrike.of(1.0001d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DeltaStrike test = DeltaStrike.of(0.6d);
    coverImmutableBean(test);
    DeltaStrike test2 = DeltaStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    DeltaStrike test = DeltaStrike.of(0.6d);
    assertSerialization(test);
  }

}
