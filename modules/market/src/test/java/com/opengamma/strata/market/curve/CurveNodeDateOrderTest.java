/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link CurveNodeDateOrder}.
 */
@Test
public class CurveNodeDateOrderTest {

  public void test_DEFAULT() {
    CurveNodeDateOrder test = CurveNodeDateOrder.DEFAULT;
    assertEquals(test.getMinGapInDays(), 1);
    assertEquals(test.getAction(), CurveNodeClashAction.EXCEPTION);
  }

  public void test_of() {
    CurveNodeDateOrder test = CurveNodeDateOrder.of(2, CurveNodeClashAction.DROP_THIS);
    assertEquals(test.getMinGapInDays(), 2);
    assertEquals(test.getAction(), CurveNodeClashAction.DROP_THIS);
  }

  public void test_of_invalid() {
    assertThrowsIllegalArg(() -> CurveNodeDateOrder.of(0, CurveNodeClashAction.DROP_THIS));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveNodeDateOrder test = CurveNodeDateOrder.of(2, CurveNodeClashAction.DROP_THIS);
    coverImmutableBean(test);
    CurveNodeDateOrder test2 = CurveNodeDateOrder.of(3, CurveNodeClashAction.DROP_OTHER);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveNodeDateOrder test = CurveNodeDateOrder.of(2, CurveNodeClashAction.DROP_THIS);
    assertSerialization(test);
  }

}
