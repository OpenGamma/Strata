/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_1W;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.observation.IborRateObservation;

/**
 * Test ExpandedIborFuture.
 */
@Test
public class ExpandedIborFutureTest {

  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR_3M = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 3, 15);
  private static final int ROUNDING = 6;
  private static final IborRateObservation RATE_OBS_GBP = IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1);
  private static final IborRateObservation RATE_OBS_USD = IborRateObservation.of(USD_LIBOR_3M, LAST_TRADE_DATE_2);
  private static final IborRateObservation RATE_OBS_GBP_1W = IborRateObservation.of(GBP_LIBOR_1W, LAST_TRADE_DATE_1);

  private static final double TOL = 1.0e-13;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ExpandedIborFuture test = ExpandedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .rate(RATE_OBS_GBP)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertEquals(test.getNotional(), NOTIONAL_1);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR_2M);
    assertEquals(test.getRoundingDecimalPlaces(), ROUNDING);
    assertEquals(test.getRate(), RATE_OBS_GBP);
    assertEquals(test.expand(), test); // returns itself
  }

  public void test_builder_accrualFactorDefaultedFromIndex() {
    ExpandedIborFuture expIborFuture = ExpandedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .rate(RATE_OBS_GBP)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertEquals(ACCRUAL_FACTOR_2M, expIborFuture.getAccrualFactor(), TOL);
  }

  public void test_builder_zeroNotionalByDefault() {
    ExpandedIborFuture expIborFuture = ExpandedIborFuture.builder()
        .currency(GBP)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .rate(RATE_OBS_GBP)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertEquals(0.0, expIborFuture.getNotional());
  }

  public void test_builder_currencyDefaultedFromIndex() {
    ExpandedIborFuture expIborFuture = ExpandedIborFuture.builder()
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .rate(RATE_OBS_GBP)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertEquals(GBP, expIborFuture.getCurrency());
  }

  public void test_builder_exceptionWhenNoRate() {
    assertThrowsIllegalArg(() -> ExpandedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .roundingDecimalPlaces(ROUNDING)
        .build());
  }

  public void test_builder_exceptionWhenNoRateIndexOrAccrualFactor() {
    assertThrowsIllegalArg(() -> ExpandedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .roundingDecimalPlaces(ROUNDING)
        .build());
  }

  public void test_builder_exceptionWhenWeekBasedIndexAndNoAccrualFactor() {
    assertThrowsIllegalArg(() -> ExpandedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .rate(RATE_OBS_GBP_1W)
        .roundingDecimalPlaces(ROUNDING)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedIborFuture test = ExpandedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .rate(RATE_OBS_GBP)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    coverImmutableBean(test);
    ExpandedIborFuture test2 = ExpandedIborFuture.builder()
        .currency(USD)
        .notional(NOTIONAL_2)
        .accrualFactor(ACCRUAL_FACTOR_3M)
        .rate(RATE_OBS_USD)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ExpandedIborFuture test = ExpandedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .rate(RATE_OBS_GBP)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertSerialization(test);
  }

}
