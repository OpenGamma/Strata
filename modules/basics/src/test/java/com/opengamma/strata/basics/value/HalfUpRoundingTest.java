/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link HalfUpRounding}.
 */
@Test
public class HalfUpRoundingTest {

  public void test_ofDecimalPlaces() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(4);
    assertEquals(test.getDecimalPlaces(), 4);
    assertEquals(test.getFraction(), 0);
    assertEquals(test.toString(), "Round to 4dp");
    assertEquals(Rounding.ofDecimalPlaces(4), test);
  }

  public void test_ofDecimalPlaces_big() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(40);
    assertEquals(test.getDecimalPlaces(), 40);
    assertEquals(test.getFraction(), 0);
    assertEquals(test.toString(), "Round to 40dp");
    assertEquals(Rounding.ofDecimalPlaces(40), test);
  }

  public void test_ofDecimalPlaces_invalid() {
    assertThrowsIllegalArg(() -> HalfUpRounding.ofDecimalPlaces(-1));
    assertThrowsIllegalArg(() -> HalfUpRounding.ofDecimalPlaces(257));
  }

  public void test_ofFractionalDecimalPlaces() {
    HalfUpRounding test = HalfUpRounding.ofFractionalDecimalPlaces(4, 32);
    assertEquals(test.getDecimalPlaces(), 4);
    assertEquals(test.getFraction(), 32);
    assertEquals(test.toString(), "Round to 1/32 of 4dp");
    assertEquals(Rounding.ofFractionalDecimalPlaces(4, 32), test);
  }

  public void test_ofFractionalDecimalPlaces_invalid() {
    assertThrowsIllegalArg(() -> HalfUpRounding.ofFractionalDecimalPlaces(-1, 0));
    assertThrowsIllegalArg(() -> HalfUpRounding.ofFractionalDecimalPlaces(257, 0));
    assertThrowsIllegalArg(() -> HalfUpRounding.ofFractionalDecimalPlaces(0, -1));
    assertThrowsIllegalArg(() -> HalfUpRounding.ofFractionalDecimalPlaces(0, 257));
  }

  public void test_builder() {
    HalfUpRounding test = HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 4)
        .set(HalfUpRounding.meta().fraction(), 1)
        .build();
    assertEquals(test.getDecimalPlaces(), 4);
    assertEquals(test.getFraction(), 0);
    assertEquals(test.toString(), "Round to 4dp");
  }

  public void test_builder_invalid() {
    assertThrowsIllegalArg(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), -1)
        .build());
    assertThrowsIllegalArg(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 257)
        .build());
    assertThrowsIllegalArg(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 4)
        .set(HalfUpRounding.meta().fraction(), -1)
        .build());
    assertThrowsIllegalArg(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 4)
        .set(HalfUpRounding.meta().fraction(), 257)
        .build());
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "round")
  Object[][] data_round() {
    return new Object[][] {
        {HalfUpRounding.ofDecimalPlaces(2), 12.3449, 12.34},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3450, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3451, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3500, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3549, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3550, 12.36},

        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3424, 12.340},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3425, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3426, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3449, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3450, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3451, 12.345},
    };
  }

  @Test(dataProvider = "round")
  public void round_double_NONE(HalfUpRounding rounding, double input, double expected) {
    assertEquals(rounding.round(input), expected);
  }

  @Test(dataProvider = "round")
  public void round_BigDecimal_NONE(HalfUpRounding rounding, double input, double expected) {
    assertEquals(rounding.round(BigDecimal.valueOf(input)), BigDecimal.valueOf(expected));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(4);
    coverImmutableBean(test);
    HalfUpRounding test2 = HalfUpRounding.ofFractionalDecimalPlaces(4, 32);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(4);
    assertSerialization(test);
  }

}
