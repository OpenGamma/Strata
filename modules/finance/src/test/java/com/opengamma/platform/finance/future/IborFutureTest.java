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

/**
 * Test IborFuture.
 */
@Test
public class IborFutureTest {

  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR_3M = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 3, 15);
  private static final int ROUNDING = 6;

  //-------------------------------------------------------------------------
  public void test_builder() {
    IborFuture iborFuture = IborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_1)
        .index(GBP_LIBOR_2M)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertEquals(GBP, iborFuture.getCurrency());
    assertEquals(NOTIONAL_1, iborFuture.getNotional());
    assertEquals(ACCRUAL_FACTOR_2M, iborFuture.getAccrualFactor());
    assertEquals(LAST_TRADE_DATE_1, iborFuture.getLastTradeDate());
    assertEquals(GBP_LIBOR_2M, iborFuture.getIndex());
    assertEquals(ROUNDING, iborFuture.getRoundingDecimalPlaces());
  }

  public void test_builder_defaults() {
    IborFuture iborFuture = IborFuture.builder()
        .currency(GBP)
        .lastTradeDate(LAST_TRADE_DATE_1)
        .index(GBP_LIBOR_2M)
        .build();
    assertEquals(GBP, iborFuture.getCurrency());
    assertEquals(0.0, iborFuture.getNotional());
    assertEquals(ACCRUAL_FACTOR_2M, iborFuture.getAccrualFactor());
    assertEquals(LAST_TRADE_DATE_1, iborFuture.getLastTradeDate());
    assertEquals(GBP_LIBOR_2M, iborFuture.getIndex());
    assertEquals(4, iborFuture.getRoundingDecimalPlaces());
  }

  public void test_builder_noIndex() {
    assertThrowsIllegalArg(() -> IborFuture.builder()
        .notional(NOTIONAL_1)
        .currency(GBP)
        .lastTradeDate(LAST_TRADE_DATE_1)
        .roundingDecimalPlaces(ROUNDING)
        .build());
  }

  public void test_builder_noCurrency() {
    IborFuture iborFuture = IborFuture.builder()
        .notional(NOTIONAL_1)
        .index(GBP_LIBOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_1)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertEquals(GBP, iborFuture.getCurrency());
  }

  public void test_builder_noLastTradeDate() {
    assertThrowsIllegalArg(() -> IborFuture.builder()
        .notional(NOTIONAL_1)
        .currency(GBP)
        .index(GBP_LIBOR_2M)
        .roundingDecimalPlaces(ROUNDING)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFuture iborFuture1 = IborFuture.builder()
        .currency(USD)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_3M)
        .lastTradeDate(LAST_TRADE_DATE_1)
        .index(USD_LIBOR_3M)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    coverImmutableBean(iborFuture1);
    IborFuture iborFuture2 = IborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_2)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_2)
        .index(GBP_LIBOR_2M)
        .build();
    coverBeanEquals(iborFuture1, iborFuture2);
  }

  public void test_serialization() {
    IborFuture iborFuture = IborFuture.builder()
        .currency(USD)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_1)
        .index(GBP_LIBOR_2M)
        .roundingDecimalPlaces(ROUNDING)
        .build();
    assertSerialization(iborFuture);
  }

}
