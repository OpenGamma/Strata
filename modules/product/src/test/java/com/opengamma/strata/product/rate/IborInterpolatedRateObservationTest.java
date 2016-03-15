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

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Test.
 */
@Test
public class IborInterpolatedRateObservationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final IborIndexObservation GBP_LIBOR_1W_OBS = IborIndexObservation.of(GBP_LIBOR_1W, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_1M_OBS = IborIndexObservation.of(GBP_LIBOR_1M, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_OBS = IborIndexObservation.of(GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation EUR_EURIBOR_1W_OBS = IborIndexObservation.of(EUR_EURIBOR_1W, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation EUR_EURIBOR_2W_OBS = IborIndexObservation.of(EUR_EURIBOR_2W, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_OBS2 =
      IborIndexObservation.of(GBP_LIBOR_3M, FIXING_DATE.plusDays(1), REF_DATA);

  //-------------------------------------------------------------------------
  public void test_of_monthly() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    assertEquals(test.getShortObservation(), GBP_LIBOR_1M_OBS);
    assertEquals(test.getLongObservation(), GBP_LIBOR_3M_OBS);
    assertEquals(test.getFixingDate(), FIXING_DATE);
  }

  public void test_of_monthly_byObs() {
    IborInterpolatedRateObservation test = IborInterpolatedRateObservation.of(GBP_LIBOR_1M_OBS, GBP_LIBOR_3M_OBS);
    assertEquals(test.getShortObservation(), GBP_LIBOR_1M_OBS);
    assertEquals(test.getLongObservation(), GBP_LIBOR_3M_OBS);
    assertEquals(test.getFixingDate(), FIXING_DATE);
  }

  public void test_of_monthly_reverseOrder() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_1M, FIXING_DATE, REF_DATA);
    assertEquals(test.getShortObservation(), GBP_LIBOR_1M_OBS);
    assertEquals(test.getLongObservation(), GBP_LIBOR_3M_OBS);
    assertEquals(test.getFixingDate(), FIXING_DATE);
  }

  public void test_of_weekly() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(EUR_EURIBOR_1W, EUR_EURIBOR_2W, FIXING_DATE, REF_DATA);
    assertEquals(test.getShortObservation(), EUR_EURIBOR_1W_OBS);
    assertEquals(test.getLongObservation(), EUR_EURIBOR_2W_OBS);
    assertEquals(test.getFixingDate(), FIXING_DATE);
  }

  public void test_of_weekly_reverseOrder() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(EUR_EURIBOR_2W, EUR_EURIBOR_1W, FIXING_DATE, REF_DATA);
    assertEquals(test.getShortObservation(), EUR_EURIBOR_1W_OBS);
    assertEquals(test.getLongObservation(), EUR_EURIBOR_2W_OBS);
    assertEquals(test.getFixingDate(), FIXING_DATE);
  }

  public void test_of_weekMonthCombination() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1W, GBP_LIBOR_1M, FIXING_DATE, REF_DATA);
    assertEquals(test.getShortObservation(), GBP_LIBOR_1W_OBS);
    assertEquals(test.getLongObservation(), GBP_LIBOR_1M_OBS);
    assertEquals(test.getFixingDate(), FIXING_DATE);
  }

  public void test_of_sameIndex() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_1M, FIXING_DATE, REF_DATA));
  }

  public void test_builder_indexOrder() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.meta().builder()
        .set(IborInterpolatedRateObservation.meta().shortObservation(), GBP_LIBOR_3M_OBS)
        .set(IborInterpolatedRateObservation.meta().longObservation(), GBP_LIBOR_1M_OBS)
        .build());
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.meta().builder()
        .set(IborInterpolatedRateObservation.meta().shortObservation(), EUR_EURIBOR_2W_OBS)
        .set(IborInterpolatedRateObservation.meta().longObservation(), EUR_EURIBOR_1W_OBS)
        .build());
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(EUR_EURIBOR_2W_OBS, EUR_EURIBOR_1W_OBS));
  }

  public void test_of_differentCurrencies() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.of(EUR_EURIBOR_2W, GBP_LIBOR_1M, FIXING_DATE, REF_DATA));
  }

  public void test_of_differentFixingDates() {
    assertThrowsIllegalArg(() -> IborInterpolatedRateObservation.meta().builder()
        .set(IborInterpolatedRateObservation.meta().shortObservation(), GBP_LIBOR_1M_OBS)
        .set(IborInterpolatedRateObservation.meta().longObservation(), GBP_LIBOR_3M_OBS2)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_1M, GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    coverImmutableBean(test);
    IborInterpolatedRateObservation test2 =
        IborInterpolatedRateObservation.of(USD_LIBOR_1M, USD_LIBOR_3M, date(2014, 7, 30), REF_DATA);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborInterpolatedRateObservation test =
        IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    assertSerialization(test);
  }

}
