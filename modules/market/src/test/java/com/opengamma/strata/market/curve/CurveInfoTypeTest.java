/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;

/**
 * Test {@link CurveInfoType}.
 */
@Test
public class CurveInfoTypeTest {

  public void test_DAY_COUNT() {
    CurveInfoType<DayCount> test = CurveInfoType.DAY_COUNT;
    assertEquals(test.toString(), "DayCount");
  }

  public void test_JACOBIAN() {
    CurveInfoType<JacobianCalibrationMatrix> test = CurveInfoType.JACOBIAN;
    assertEquals(test.toString(), "Jacobian");
  }

  public void coverage() {
    CurveInfoType<String> test = CurveInfoType.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
