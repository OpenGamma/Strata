/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.OptionalDouble;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class IborAveragedFixingTest {

  public void test_of_date() {
    IborAveragedFixing test = IborAveragedFixing.of(date(2014, 6, 30));
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .fixingDate(date(2014, 6, 30))
        .fixedRate(null)
        .weight(1)
        .build();
    assertEquals(test, expected);
  }

  public void test_of_date_fixedRate() {
    IborAveragedFixing test = IborAveragedFixing.of(date(2014, 6, 30), 0.05);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .fixingDate(date(2014, 6, 30))
        .fixedRate(0.05)
        .weight(1)
        .build();
    assertEquals(test, expected);
    assertEquals(test.getFixedRate(), OptionalDouble.of(0.05));
  }

  public void test_of_date_fixedRate_null() {
    IborAveragedFixing test = IborAveragedFixing.of(date(2014, 6, 30), null);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .fixingDate(date(2014, 6, 30))
        .fixedRate(null)
        .weight(1)
        .build();
    assertEquals(test, expected);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
  }

  public void test_of_date_null() {
    assertThrowsIllegalArg(() -> IborAveragedFixing.of(null));
    assertThrowsIllegalArg(() -> IborAveragedFixing.of(null, 0.05));
    assertThrowsIllegalArg(() -> IborAveragedFixing.of(null, null));
  }

  //-------------------------------------------------------------------------
  public void test_ofDaysInResetPeriod() {
    IborAveragedFixing test = IborAveragedFixing.ofDaysInResetPeriod(
        date(2014, 6, 30), date(2014, 7, 2), date(2014, 8, 2));
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .fixingDate(date(2014, 6, 30))
        .fixedRate(null)
        .weight(31)
        .build();
    assertEquals(test, expected);
  }

  public void test_ofDaysInResetPeriod_fixedRate() {
    IborAveragedFixing test = IborAveragedFixing.ofDaysInResetPeriod(
        date(2014, 6, 30), date(2014, 7, 2), date(2014, 9, 2), 0.06);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .fixingDate(date(2014, 6, 30))
        .fixedRate(0.06)
        .weight(62)
        .build();
    assertEquals(test, expected);
    assertEquals(test.getFixedRate(), OptionalDouble.of(0.06));
  }

  public void test_ofDaysInResetPeriod_fixedRate_null() {
    IborAveragedFixing test = IborAveragedFixing.ofDaysInResetPeriod(
        date(2014, 6, 30), date(2014, 7, 2), date(2014, 9, 2), null);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .fixingDate(date(2014, 6, 30))
        .fixedRate(null)
        .weight(62)
        .build();
    assertEquals(test, expected);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
  }

  public void test_ofDaysInResetPeriod_null() {
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(null, date(2014, 7, 2), date(2014, 8, 2)));
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(date(2014, 6, 30), null, date(2014, 8, 2)));
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(date(2014, 6, 30), date(2014, 7, 2), null));
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(null, null, null));
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(null, date(2014, 7, 2), date(2014, 8, 2), 0.05));
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(date(2014, 6, 30), null, date(2014, 8, 2), 0.05));
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(date(2014, 6, 30), date(2014, 7, 2), null, 0.05));
    assertThrowsIllegalArg(() -> IborAveragedFixing.ofDaysInResetPeriod(null, null, null, null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborAveragedFixing test = IborAveragedFixing.of(date(2014, 6, 30));
    coverImmutableBean(test);
  }

  public void test_serialization() {
    IborAveragedFixing test = IborAveragedFixing.of(date(2014, 6, 30));
    assertSerialization(test);
  }

}
