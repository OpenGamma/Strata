/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.future;

import static com.opengamma.basics.PutCall.CALL;
import static com.opengamma.basics.PutCall.PUT;
import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test IborFutureOption. 
 */
@Test
public class IborFutureOptionTest {

  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 9, 16);
  private static final int ROUNDING = 6;
  private static final IborFuture IBOR_FUTURE_1 = IborFuture.builder()
      .currency(GBP)
      .index(GBP_LIBOR_2M)
      .notional(NOTIONAL_1)
      .lastTradeDate(LAST_TRADE_DATE_1)
      .roundingDecimalPlaces(ROUNDING)
      .build();
  private static final IborFuture IBOR_FUTURE_2 = IborFuture.builder()
      .index(GBP_LIBOR_3M)
      .notional(NOTIONAL_2)
      .lastTradeDate(LAST_TRADE_DATE_2)
      .build();

  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final double STRIKE_PRICE = 1.075;

  //-------------------------------------------------------------------------
  public void test_builder() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(IBOR_FUTURE_1)
        .build();
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getExpirationDate(), EXPIRY_DATE);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getIborFuture(), IBOR_FUTURE_1);
  }

  public void test_builder_expiryNotAfterTradeDate() {
    assertThrowsIllegalArg(() -> IborFutureOption.builder()
        .putCall(CALL)
        .iborFuture(IBOR_FUTURE_1)
        .expirationDate(LAST_TRADE_DATE_2)
        .strikePrice(STRIKE_PRICE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(IBOR_FUTURE_1)
        .build();
    assertEquals(test.getIborFuture(), IBOR_FUTURE_1);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getExpirationDate(), EXPIRY_DATE);
    ExpandedIborFutureOption expanded = test.expand();
    assertEquals(expanded.getIborFuture(), IBOR_FUTURE_1.expand());
    assertEquals(expanded.getStrikePrice(), STRIKE_PRICE);
    assertEquals(expanded.getExpirationDate(), EXPIRY_DATE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(IBOR_FUTURE_1)
        .build();
    coverImmutableBean(test);
    IborFutureOption test2 = IborFutureOption.builder()
        .putCall(PUT)
        .expirationDate(LAST_TRADE_DATE_1)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(IBOR_FUTURE_2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureOption test = IborFutureOption.builder()
        .putCall(CALL)
        .expirationDate(EXPIRY_DATE)
        .strikePrice(STRIKE_PRICE)
        .iborFuture(IBOR_FUTURE_1)
        .build();
    assertSerialization(test);
  }

}
