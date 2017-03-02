/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ValueAdjustment}.
 */
@Test
public class ValueAdjustmentTest {

  private static double TOLERANCE = 0.0001d;

  public void test_NONE() {
    ValueAdjustment test = ValueAdjustment.NONE;
    assertEquals(test.getModifyingValue(), 0, TOLERANCE);
    assertEquals(test.getType(), ValueAdjustmentType.DELTA_AMOUNT);
    assertEquals(test.adjust(100), 100, TOLERANCE);
    assertEquals(test.toString(), "ValueAdjustment[result = input]");
  }

  public void test_ofReplace() {
    ValueAdjustment test = ValueAdjustment.ofReplace(200);
    assertEquals(test.getModifyingValue(), 200, TOLERANCE);
    assertEquals(test.getType(), ValueAdjustmentType.REPLACE);
    assertEquals(test.adjust(100), 200, TOLERANCE);
    assertEquals(test.toString(), "ValueAdjustment[result = 200.0]");
  }

  public void test_ofDeltaAmount() {
    ValueAdjustment test = ValueAdjustment.ofDeltaAmount(20);
    assertEquals(test.getModifyingValue(), 20, TOLERANCE);
    assertEquals(test.getType(), ValueAdjustmentType.DELTA_AMOUNT);
    assertEquals(test.adjust(100), 120, TOLERANCE);
    assertEquals(test.toString(), "ValueAdjustment[result = input + 20.0]");
  }

  public void test_ofDeltaMultiplier() {
    ValueAdjustment test = ValueAdjustment.ofDeltaMultiplier(0.1);
    assertEquals(test.getModifyingValue(), 0.1, TOLERANCE);
    assertEquals(test.getType(), ValueAdjustmentType.DELTA_MULTIPLIER);
    assertEquals(test.adjust(100), 110, TOLERANCE);
    assertEquals(test.toString(), "ValueAdjustment[result = input + input * 0.1]");
  }

  public void test_ofMultiplier() {
    ValueAdjustment test = ValueAdjustment.ofMultiplier(1.1);
    assertEquals(test.getModifyingValue(), 1.1, TOLERANCE);
    assertEquals(test.getType(), ValueAdjustmentType.MULTIPLIER);
    assertEquals(test.adjust(100), 110, TOLERANCE);
    assertEquals(test.toString(), "ValueAdjustment[result = input * 1.1]");
  }

  //-------------------------------------------------------------------------
  public void equals() {
    ValueAdjustment a1 = ValueAdjustment.ofReplace(200);
    ValueAdjustment a2 = ValueAdjustment.ofReplace(200);
    ValueAdjustment b = ValueAdjustment.ofDeltaMultiplier(200);
    ValueAdjustment c = ValueAdjustment.ofDeltaMultiplier(0.1);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ValueAdjustment.ofReplace(200));
  }

  public void test_serialization() {
    assertSerialization(ValueAdjustment.ofReplace(200));
  }

}
