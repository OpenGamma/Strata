/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedTermDepositTrade}. 
 */
@Test
public class ResolvedTermDepositTradeTest {

  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 20);
  private static final double YEAR_FRACTION = ACT_365F.yearFraction(START_DATE, END_DATE);
  private static final double PRINCIPAL = 100000000d;
  private static final double RATE = 0.0250;

  private static final ResolvedTermDeposit DEPOSIT = ResolvedTermDeposit.builder()
      .currency(GBP)
      .notional(PRINCIPAL)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .yearFraction(YEAR_FRACTION)
      .rate(RATE)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedTermDepositTrade test = ResolvedTermDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    ResolvedTermDepositTrade test = ResolvedTermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedTermDepositTrade test1 = ResolvedTermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    ResolvedTermDepositTrade test2 = ResolvedTermDepositTrade.builder()
        .product(DEPOSIT)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedTermDepositTrade test = ResolvedTermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
