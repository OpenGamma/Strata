/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate;

import static com.opengamma.basics.index.IborIndices.EUR_EURIBOR_1W;
import static com.opengamma.basics.index.IborIndices.EUR_EURIBOR_2W;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_1W;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class IborInterpolatedRateTest {

  public void test_of_monthly() {
    IborInterpolatedRate test = IborInterpolatedRate.of(GBP_LIBOR_1M, GBP_LIBOR_3M, date(2014, 6, 30));
    IborInterpolatedRate expected = IborInterpolatedRate.builder()
        .shortIndex(GBP_LIBOR_1M)
        .longIndex(GBP_LIBOR_3M)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_weekly() {
    IborInterpolatedRate test = IborInterpolatedRate.of(EUR_EURIBOR_1W, EUR_EURIBOR_2W, date(2014, 6, 30));
    IborInterpolatedRate expected = IborInterpolatedRate.builder()
        .shortIndex(EUR_EURIBOR_1W)
        .longIndex(EUR_EURIBOR_2W)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_weekMonthCombination() {
    IborInterpolatedRate test = IborInterpolatedRate.of(GBP_LIBOR_1W, GBP_LIBOR_1M, date(2014, 6, 30));
    IborInterpolatedRate expected = IborInterpolatedRate.builder()
        .shortIndex(GBP_LIBOR_1W)
        .longIndex(GBP_LIBOR_1M)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_sameIndex() {
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(GBP_LIBOR_1M, GBP_LIBOR_1M, date(2014, 6, 30)));
  }

  public void test_of_indexOrderMonthly() {
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(GBP_LIBOR_3M, GBP_LIBOR_1M, date(2014, 6, 30)));
  }

  public void test_of_indexOrderWeekly() {
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(EUR_EURIBOR_2W, EUR_EURIBOR_1W, date(2014, 6, 30)));
  }

  public void test_of_differentCurrencies() {
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(EUR_EURIBOR_2W, GBP_LIBOR_1M, date(2014, 6, 30)));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(null, GBP_LIBOR_3M, date(2014, 6, 30)));
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(GBP_LIBOR_1M, null, date(2014, 6, 30)));
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(GBP_LIBOR_1M, GBP_LIBOR_3M, null));
    assertThrowsIllegalArg(() -> IborInterpolatedRate.of(null, null, null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborInterpolatedRate test = IborInterpolatedRate.of(GBP_LIBOR_1M, GBP_LIBOR_3M, date(2014, 6, 30));
    coverImmutableBean(test);
    IborInterpolatedRate test2 = IborInterpolatedRate.of(USD_LIBOR_1M, USD_LIBOR_3M, date(2014, 7, 30));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborInterpolatedRate test = IborInterpolatedRate.of(GBP_LIBOR_1M, GBP_LIBOR_3M, date(2014, 6, 30));
    assertSerialization(test);
  }

}
