/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.future;

import static com.opengamma.basics.PutCall.CALL;
import static com.opengamma.basics.PutCall.PUT;
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
 * Test ExpandedIborFutureOption. 
 */
@Test
public class ExpandedIborFutureOptionTest {

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

  private static final ExpandedIborFuture EXPANDED_IBOR_FUTURE_1 = ExpandedIborFuture.builder()
      .currency(GBP)
      .notional(NOTIONAL_1)
      .accrualFactor(ACCRUAL_FACTOR_2M)
      .rate(RATE_OBS_GBP)
      .roundingDecimalPlaces(ROUNDING)
      .build();
  private static final ExpandedIborFuture EXPANDED_IBOR_FUTURE_2 = ExpandedIborFuture.builder()
      .currency(GBP)
      .notional(NOTIONAL_2)
      .accrualFactor(ACCRUAL_FACTOR_3M)
      .rate(RATE_OBS_GBP_1W)
      .build();

  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final double STRIKE_PRICE = 1.075;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ExpandedIborFutureOption test = ExpandedIborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(EXPANDED_IBOR_FUTURE_1)
        .build();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getExpirationDate(), EXPIRY_DATE);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getIborFuture(), EXPANDED_IBOR_FUTURE_1);
    assertEquals(test.expand(), test);
  }

  public void test_builder_expiryNotAfterTradeDate() {
    ExpandedIborFuture test = ExpandedIborFuture.builder()
        .currency(USD)
        .notional(NOTIONAL_2)
        .accrualFactor(ACCRUAL_FACTOR_3M)
        .rate(RATE_OBS_USD)
        .build();
    assertThrowsIllegalArg(() -> ExpandedIborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(test)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedIborFutureOption test = ExpandedIborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(EXPANDED_IBOR_FUTURE_1)
        .build();
    coverImmutableBean(test);
    ExpandedIborFutureOption test2 = ExpandedIborFutureOption.builder()
        .putCall(PUT)
        .expirationDate(LAST_TRADE_DATE_1)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(EXPANDED_IBOR_FUTURE_2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ExpandedIborFutureOption test = ExpandedIborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(EXPANDED_IBOR_FUTURE_1)
        .build();
    assertSerialization(test);
  }

}
