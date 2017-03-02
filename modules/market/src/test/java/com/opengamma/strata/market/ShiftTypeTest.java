/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ShiftType}.
 */
@Test
public class ShiftTypeTest {

  public void test_applyShift() {
    assertEquals(ShiftType.ABSOLUTE.applyShift(2, 0.1), 2.1);
    assertEquals(ShiftType.RELATIVE.applyShift(2, 0.1), 2.2);
    assertEquals(ShiftType.SCALED.applyShift(2, 1.1), 2.2);
  }

  public void test_toValueAdjustment() {
    assertEquals(ShiftType.ABSOLUTE.toValueAdjustment(0.1).adjust(2), 2.1);
    assertEquals(ShiftType.RELATIVE.toValueAdjustment(0.1).adjust(2), 2.2);
    assertEquals(ShiftType.SCALED.toValueAdjustment(1.1).adjust(2), 2.2);
  }

  public void test_name() {
    assertEquals(ShiftType.ABSOLUTE.name(), "ABSOLUTE");
    assertEquals(ShiftType.RELATIVE.name(), "RELATIVE");
    assertEquals(ShiftType.SCALED.name(), "SCALED");
  }

  public void test_toString() {
    assertEquals(ShiftType.ABSOLUTE.toString(), "Absolute");
    assertEquals(ShiftType.RELATIVE.toString(), "Relative");
    assertEquals(ShiftType.SCALED.toString(), "Scaled");
  }

  public void coverage() {
    coverEnum(ShiftType.class);
  }

}
