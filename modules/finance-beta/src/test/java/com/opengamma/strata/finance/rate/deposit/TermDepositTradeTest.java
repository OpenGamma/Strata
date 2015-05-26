/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

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
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.TradeInfo;

/**
 * Test {@link TermDepositTrade}.
 */
@Test
public class TermDepositTradeTest {

  private static final TermDeposit DEPOSIT = TermDeposit.builder()
      .buySell(BuySell.BUY)
      .startDate(LocalDate.of(2015, 1, 19))
      .endDate(LocalDate.of(2015, 7, 19))
      .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
      .dayCount(ACT_365F)
      .principal(CurrencyAmount.of(GBP, 100000000))
      .rate(0.0250)
      .build();
  private static final StandardId STANDARD_ID = StandardId.of("OG-Trade", "1");
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2014, 6, 30)).build();

  public void test_builder() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .standardId(STANDARD_ID)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), DEPOSIT);
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void coverage() {
    TermDepositTrade test1 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .standardId(STANDARD_ID)
        .tradeInfo(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    TermDepositTrade test2 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .standardId(StandardId.of("OG-Trade", "2"))
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .standardId(STANDARD_ID)
        .tradeInfo(TRADE_INFO)
        .build();
    assertSerialization(test);
  }
}
