/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

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
 * Test {@link ResolvedOvernightInArrearsCapFloorTrade}.
 */
public class ResolvedOvernightInArrearsCapFloorTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final ResolvedOvernightInArrearsCapFloor PRODUCT = ResolvedOvernightInArrearsCapFloor.of(
      ResolvedOvernightInArrearsCapFloorTest.CAPFLOOR_LEG,
      ResolvedOvernightInArrearsCapFloorTest.PAY_LEG);
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, -0.001 * 1.0e6), date(2016, 7, 2));

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedOvernightInArrearsCapFloorTrade test = ResolvedOvernightInArrearsCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getPremium()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_builder_full() {
    ResolvedOvernightInArrearsCapFloorTrade test = ResolvedOvernightInArrearsCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getPremium()).isEqualTo(Optional.of(PREMIUM));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedOvernightInArrearsCapFloorTrade test = ResolvedOvernightInArrearsCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    coverImmutableBean(test);
    ResolvedOvernightInArrearsCapFloorTrade test2 = ResolvedOvernightInArrearsCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedOvernightInArrearsCapFloorTrade test = ResolvedOvernightInArrearsCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    assertSerialization(test);
  }

}
