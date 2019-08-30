/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.payment;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedBulletPaymentTrade}.
 */
public class ResolvedBulletPaymentTradeTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final ResolvedBulletPayment PRODUCT1 = ResolvedBulletPayment.of(Payment.of(GBP_P1000, DATE_2015_06_30));
  private static final ResolvedBulletPayment PRODUCT2 = ResolvedBulletPayment.of(Payment.of(GBP_M1000, DATE_2015_06_30));
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ResolvedBulletPaymentTrade test = ResolvedBulletPaymentTrade.of(TRADE_INFO, PRODUCT1);
    assertThat(test.getProduct()).isEqualTo(PRODUCT1);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    ResolvedBulletPaymentTrade test = ResolvedBulletPaymentTrade.builder()
        .product(PRODUCT1)
        .build();
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
    assertThat(test.getProduct()).isEqualTo(PRODUCT1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedBulletPaymentTrade test = ResolvedBulletPaymentTrade.builder()
        .info(TradeInfo.of(date(2014, 6, 30)))
        .product(PRODUCT1)
        .build();
    coverImmutableBean(test);
    ResolvedBulletPaymentTrade test2 = ResolvedBulletPaymentTrade.builder()
        .product(PRODUCT2)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedBulletPaymentTrade test = ResolvedBulletPaymentTrade.builder()
        .info(TradeInfo.of(date(2014, 6, 30)))
        .product(PRODUCT1)
        .build();
    assertSerialization(test);
  }

}
