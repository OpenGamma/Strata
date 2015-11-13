/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_1W;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_2W;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1W;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
@Test
public class IborInterpolatedRateObservationTest {

  public void test_of_monthly() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, date(2014, 6, 30));
    IborInterpolatedRateObservation expected = IborInterpolatedRateObservation.builder()
        .shortIndex(GBP_LIBOR_1M)
        .longIndex(GBP_LIBOR_3M)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_monthly_reverseOrder() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_1M, date(2014, 6, 30));
    IborInterpolatedRateObservation expected = IborInterpolatedRateObservation.builder()
        .shortIndex(GBP_LIBOR_1M)
        .longIndex(GBP_LIBOR_3M)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_weekly() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(EUR_EURIBOR_1W, EUR_EURIBOR_2W, date(2014, 6, 30));
    IborInterpolatedRateObservation expected = IborInterpolatedRateObservation.builder()
        .shortIndex(EUR_EURIBOR_1W)
        .longIndex(EUR_EURIBOR_2W)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_weekly_reverseOrder() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(EUR_EURIBOR_2W, EUR_EURIBOR_1W, date(2014, 6, 30));
    IborInterpolatedRateObservation expected = IborInterpolatedRateObservation.builder()
        .shortIndex(EUR_EURIBOR_1W)
        .longIndex(EUR_EURIBOR_2W)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_weekMonthCombination() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1W, GBP_LIBOR_1M, date(2014, 6, 30));
    IborInterpolatedRateObservation expected = IborInterpolatedRateObservation.builder()
        .shortIndex(GBP_LIBOR_1W)
        .longIndex(GBP_LIBOR_1M)
        .fixingDate(date(2014, 6, 30))
        .build();
    assertEquals(test, expected);
  }

  public void test_of_sameIndex() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_1M, date(2014, 6, 30)));
  }

  public void test_builder_indexOrder() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.builder()
        .shortIndex(GBP_LIBOR_3M)
        .longIndex(GBP_LIBOR_1M)
        .fixingDate(date(2014, 6, 30))
        .build());
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.builder()
        .shortIndex(EUR_EURIBOR_2W)
        .longIndex(EUR_EURIBOR_1W)
        .fixingDate(date(2014, 6, 30))
        .build());
  }

  public void test_of_differentCurrencies() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(EUR_EURIBOR_2W, GBP_LIBOR_1M, date(2014, 6, 30)));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(null, GBP_LIBOR_3M, date(2014, 6, 30)));
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(GBP_LIBOR_1M, null, date(2014, 6, 30)));
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, null));
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(null, null, null));
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, date(2014, 6, 30));
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_1M, GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, date(2014, 6, 30));
    coverImmutableBean(test);
    IborInterpolatedRateObservation test2 =
        IborInterpolatedRateObservation.of(USD_LIBOR_1M, USD_LIBOR_3M, date(2014, 7, 30));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, date(2014, 6, 30));
    assertSerialization(test);
  }

}
