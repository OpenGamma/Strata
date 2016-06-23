/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedFxVanillaOptionTrade}.
 */
@Test
public class ResolvedFxVanillaOptionTradeTest {

  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final ResolvedFxVanillaOption OPTION = ResolvedFxVanillaOptionTest.sut();
  private static final ResolvedFxVanillaOption OPTION2 = ResolvedFxVanillaOptionTest.sut();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));
  private static final Payment PREMIUM = Payment.of(EUR_AMOUNT, PAYMENT_DATE);
  private static final Payment PREMIUM2 = Payment.of(EUR_AMOUNT, PAYMENT_DATE.plusDays(1));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxVanillaOptionTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), OPTION);
    assertEquals(test.getPremium(), PREMIUM);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedFxVanillaOptionTrade sut() {
    return ResolvedFxVanillaOptionTrade.builder()
        .info(TRADE_INFO)
        .product(OPTION)
        .premium(PREMIUM)
        .build();
  }

  static ResolvedFxVanillaOptionTrade sut2() {
    return ResolvedFxVanillaOptionTrade.builder()
        .product(OPTION2)
        .premium(PREMIUM2)
        .build();
  }

}
