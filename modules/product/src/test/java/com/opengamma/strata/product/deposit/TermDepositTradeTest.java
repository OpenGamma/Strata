/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link TermDepositTrade}. 
 */
@Test
public class TermDepositTradeTest {

  private static final TermDeposit DEPOSIT = TermDeposit.builder()
      .buySell(BuySell.BUY)
      .currency(GBP)
      .notional(100000000d)
      .startDate(LocalDate.of(2015, 1, 19))
      .endDate(LocalDate.of(2015, 7, 19))
      .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
      .dayCount(ACT_365F)
      .rate(0.0250)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2014, 6, 30)).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TermDepositTrade test1 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .tradeInfo(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    TermDepositTrade test2 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .tradeInfo(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
