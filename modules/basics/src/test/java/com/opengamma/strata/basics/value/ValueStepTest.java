/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.OptionalInt;

import org.testng.annotations.Test;

/**
 * Test {@link ValueStep}.
 */
@Test
public class ValueStepTest {

  private static ValueAdjustment DELTA_MINUS_2000 = ValueAdjustment.ofDeltaAmount(-2000);
  private static ValueAdjustment ABSOLUTE_100 = ValueAdjustment.ofReplace(100);

  public void test_of_intAdjustment() {
    ValueStep test = ValueStep.of(2, DELTA_MINUS_2000);
    assertEquals(test.getDate(), Optional.empty());
    assertEquals(test.getPeriodIndex(), OptionalInt.of(2));
    assertEquals(test.getValue(), DELTA_MINUS_2000);
  }

  public void test_of_dateAdjustment() {
    ValueStep test = ValueStep.of(date(2014, 6, 30), DELTA_MINUS_2000);
    assertEquals(test.getDate(), Optional.of(date(2014, 6, 30)));
    assertEquals(test.getPeriodIndex(), OptionalInt.empty());
    assertEquals(test.getValue(), DELTA_MINUS_2000);
  }

  public void test_builder_invalid() {
    assertThrowsIllegalArg(() -> ValueStep.builder().value(DELTA_MINUS_2000).build());
    assertThrowsIllegalArg(() -> ValueStep.builder().date(date(2014, 6, 30)).periodIndex(1).value(DELTA_MINUS_2000).build());
    assertThrowsIllegalArg(() -> ValueStep.builder().periodIndex(0).value(DELTA_MINUS_2000).build());
    assertThrowsIllegalArg(() -> ValueStep.builder().periodIndex(-1).value(DELTA_MINUS_2000).build());
  }

  //-------------------------------------------------------------------------
  public void equals() {
    ValueStep a1 = ValueStep.of(2, DELTA_MINUS_2000);
    ValueStep a2 = ValueStep.of(2, DELTA_MINUS_2000);
    ValueStep b = ValueStep.of(1, DELTA_MINUS_2000);
    ValueStep c = ValueStep.of(2, ABSOLUTE_100);
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);

    ValueStep d1 = ValueStep.of(date(2014, 6, 30), DELTA_MINUS_2000);
    ValueStep d2 = ValueStep.of(date(2014, 6, 30), DELTA_MINUS_2000);
    ValueStep e = ValueStep.of(date(2014, 7, 30), DELTA_MINUS_2000);
    ValueStep f = ValueStep.of(date(2014, 7, 30), ABSOLUTE_100);
    assertEquals(d1.equals(d1), true);
    assertEquals(d1.equals(d2), true);
    assertEquals(d1.equals(e), false);
    assertEquals(d1.equals(f), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ValueStep.of(2, DELTA_MINUS_2000));
  }

  public void test_serialization() {
    assertSerialization(ValueStep.of(2, DELTA_MINUS_2000));
  }

}
