/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedIborCapFloorTrade}.
 */
@Test
public class ResolvedIborCapFloorTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final ResolvedIborCapFloor PRODUCT =
      ResolvedIborCapFloor.of(ResolvedIborCapFloorTest.CAPFLOOR_LEG, ResolvedIborCapFloorTest.PAY_LEG);
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(EUR, -0.001 * 1.0e6), date(2016, 7, 2));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborCapFloorTrade test = ResolvedIborCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    assertEquals(test.getInfo(), TradeInfo.empty());
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getPremium(), Optional.empty());
  }

  public void test_builder_full() {
    ResolvedIborCapFloorTrade test = ResolvedIborCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getPremium(), Optional.of(PREMIUM));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedIborCapFloorTrade test = ResolvedIborCapFloorTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
    coverImmutableBean(test);
    ResolvedIborCapFloorTrade test2 = ResolvedIborCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedIborCapFloorTrade test = ResolvedIborCapFloorTrade.builder()
        .product(PRODUCT)
        .build();
    assertSerialization(test);
  }

}
