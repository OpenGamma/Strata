/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedCmsTrade}.
 */
public class ResolvedCmsTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final ResolvedCms PRODUCT = ResolvedCmsTest.sut();
  private static final ResolvedCms PRODUCT2 = ResolvedCmsTest.sut2();
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, -0.001 * 1.0e6), date(2016, 7, 2));

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedCmsTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getPremium()).isEqualTo(Optional.of(PREMIUM));
  }

  @Test
  public void test_builder_full() {
    ResolvedCmsTrade test = ResolvedCmsTrade.builder()
        .product(PRODUCT)
        .build();
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getPremium()).isEqualTo(Optional.empty());
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
  static ResolvedCmsTrade sut() {
    return ResolvedCmsTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
  }

  static ResolvedCmsTrade sut2() {
    return ResolvedCmsTrade.builder()
        .product(PRODUCT2)
        .build();
  }

}
