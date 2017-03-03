/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link EtdFutureTrade}.
 */
@Test
public class EtdFutureTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(LocalDate.of(2017, 1, 1));
  private static final EtdFutureSecurity SECURITY = EtdFutureSecurityTest.sut();

  public void test_of() {
    EtdFutureTrade test = EtdFutureTrade.of(TRADE_INFO, SECURITY, 1000, 20);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), 1000d, 0d);
    assertEquals(test.getPrice(), 20d, 0d);
    assertEquals(test.getSecurityId(), SECURITY.getSecurityId());
    assertEquals(test.getCurrency(), SECURITY.getCurrency());
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
  static EtdFutureTrade sut() {
    return EtdFutureTrade.builder()
        .info(TRADE_INFO)
        .security(SECURITY)
        .quantity(3000)
        .price(20)
        .build();
  }

  static EtdFutureTrade sut2() {
    return EtdFutureTrade.builder()
        .security(EtdFutureSecurityTest.sut2())
        .quantity(4000)
        .price(30)
        .build();
  }

}
