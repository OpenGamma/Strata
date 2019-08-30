/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedSwaptionTrade}. 
 */
public class ResolvedSwaptionTradeTest {

  private static final ResolvedSwaption SWAPTION = ResolvedSwaptionTest.sut();
  private static final ResolvedSwaption SWAPTION2 = ResolvedSwaptionTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(Currency.USD, -3150000d), date(2014, 3, 17));
  private static final Payment PREMIUM2 = Payment.of(CurrencyAmount.of(Currency.USD, -3160000d), date(2014, 3, 17));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ResolvedSwaptionTrade test = ResolvedSwaptionTrade.of(TRADE_INFO, SWAPTION, PREMIUM);
    assertThat(test.getProduct()).isEqualTo(SWAPTION);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    ResolvedSwaptionTrade test = sut();
    assertThat(test.getProduct()).isEqualTo(SWAPTION);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedSwaptionTrade sut() {
    return ResolvedSwaptionTrade.builder()
        .product(SWAPTION)
        .info(TRADE_INFO)
        .premium(PREMIUM)
        .build();
  }

  static ResolvedSwaptionTrade sut2() {
    return ResolvedSwaptionTrade.builder()
        .product(SWAPTION2)
        .premium(PREMIUM2)
        .build();
  }

}
