/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertThrowsWithCause;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link CurveNodeDate}.
 */
@Test
public class CurveNodeDateTest {

  private static final LocalDate DATE1 = date(2015, 6, 30);
  private static final LocalDate DATE2 = date(2015, 7, 1);
  private static final LocalDate DATE3 = date(2015, 7, 2);

  //-------------------------------------------------------------------------
  public void test_END() {
    CurveNodeDate test = CurveNodeDate.END;
    assertEquals(test.isFixed(), false);
    assertEquals(test.isEnd(), true);
    assertEquals(test.isLastFixing(), false);
    assertEquals(test.getType(), CurveNodeDateType.END);
    assertThrowsWithCause(() -> test.getDate(), IllegalStateException.class);
  }

  public void test_LAST_FIXING() {
    CurveNodeDate test = CurveNodeDate.LAST_FIXING;
    assertEquals(test.isFixed(), false);
    assertEquals(test.isEnd(), false);
    assertEquals(test.isLastFixing(), true);
    assertEquals(test.getType(), CurveNodeDateType.LAST_FIXING);
    assertThrowsWithCause(() -> test.getDate(), IllegalStateException.class);
  }

  public void test_of() {
    CurveNodeDate test = CurveNodeDate.of(DATE1);
    assertEquals(test.isFixed(), true);
    assertEquals(test.isEnd(), false);
    assertEquals(test.isLastFixing(), false);
    assertEquals(test.getType(), CurveNodeDateType.FIXED);
    assertEquals(test.getDate(), DATE1);
  }

  public void test_builder_fixed() {
    CurveNodeDate test = CurveNodeDate.meta().builder()
        .set(CurveNodeDate.meta().type(), CurveNodeDateType.FIXED)
        .set(CurveNodeDate.meta().date(), DATE1)
        .build();
    assertEquals(test.isFixed(), true);
    assertEquals(test.isEnd(), false);
    assertEquals(test.isLastFixing(), false);
    assertEquals(test.getType(), CurveNodeDateType.FIXED);
    assertEquals(test.getDate(), DATE1);
  }

  public void test_builder_incorrect_no_fixed_date() {
    assertThrowsIllegalArg(() -> CurveNodeDate.meta().builder()
        .set(CurveNodeDate.meta().type(), CurveNodeDateType.FIXED)
        .build());
  }

  public void test_builder_incorrect_fixed_date() {
    assertThrowsIllegalArg(() -> CurveNodeDate.meta().builder()
        .set(CurveNodeDate.meta().type(), CurveNodeDateType.LAST_FIXING)
        .set(CurveNodeDate.meta().date(), DATE1)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_calculate() {
    assertEquals(CurveNodeDate.of(DATE1).calculate(() -> DATE2, () -> DATE3), DATE1);
    assertEquals(CurveNodeDate.END.calculate(() -> DATE2, () -> DATE3), DATE2);
    assertEquals(CurveNodeDate.LAST_FIXING.calculate(() -> DATE2, () -> DATE3), DATE3);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveNodeDate test = CurveNodeDate.of(DATE1);
    coverImmutableBean(test);
    CurveNodeDate test2 = CurveNodeDate.LAST_FIXING;
    coverBeanEquals(test, test2);
    coverEnum(CurveNodeDateType.class);
  }

  public void test_serialization() {
    CurveNodeDate test = CurveNodeDate.of(DATE1);
    assertSerialization(test);
  }

}
